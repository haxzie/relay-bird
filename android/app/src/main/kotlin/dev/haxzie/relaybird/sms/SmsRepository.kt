package dev.haxzie.relaybird.sms

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import android.util.Log
import dev.haxzie.relaybird.net.WireMessage
import dev.haxzie.relaybird.net.WireThread

/**
 * Reads SMS straight out of content://sms.
 *
 * Telephony.Sms.Conversations only exposes thread_id/msg_count/snippet with no date column, and
 * Telephony.Threads has no public CONTENT_URI, so the thread list is built by grouping a single
 * date-ordered cursor client-side.
 *
 * Limits that come from not being the default SMS app: only the inbox and sent boxes are
 * readable, and there is no public API to enumerate a thread's recipients — so group/MMS threads
 * are out of scope and would otherwise render under one participant's address.
 */
class SmsRepository(private val context: Context, private val contacts: ContactNames) {

    fun threads(limit: Int = 50, watermarks: Map<Long, Long> = emptyMap()): List<WireThread> {
        val seen = LinkedHashMap<Long, WireThread>()
        query(selection = null, args = null, limit = limit * MESSAGES_PER_THREAD_SCAN)?.use { cursor ->
            val cols = Columns(cursor)
            while (cursor.moveToNext() && seen.size < limit) {
                val threadId = cursor.getLong(cols.threadId)
                if (seen.containsKey(threadId)) continue
                val address = cursor.getString(cols.address).orEmpty()
                seen[threadId] = WireThread(
                    threadId = threadId,
                    address = address,
                    displayName = contacts.nameFor(address),
                    snippet = cursor.getString(cols.body).orEmpty().take(SNIPPET_CHARS),
                    date = cursor.getLong(cols.date),
                    unread = 0,
                    latestMessageId = cursor.getLong(cols.id),
                )
            }
        }
        val unread = unreadCounts(watermarks)
        return seen.values.map { it.copy(unread = unread[it.threadId] ?: 0) }
    }

    fun messages(threadId: Long, limit: Int = 100, beforeId: Long? = null): List<WireMessage> {
        val selection = buildString {
            append("${Telephony.Sms.THREAD_ID} = ?")
            if (beforeId != null) append(" AND ${Telephony.Sms._ID} < ?")
        }
        val args = buildList {
            add(threadId.toString())
            if (beforeId != null) add(beforeId.toString())
        }.toTypedArray()

        return query(selection, args, limit)?.use { cursor ->
            val cols = Columns(cursor)
            buildList {
                while (cursor.moveToNext()) add(cursor.toWireMessage(cols))
            }
        }.orEmpty().reversed()
    }

    fun message(id: Long): WireMessage? =
        query("${Telephony.Sms._ID} = ?", arrayOf(id.toString()), 1)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toWireMessage(Columns(cursor)) else null
        }

    /** Newest message id, used to detect what the content observer actually changed. */
    fun latestId(): Long = query(null, null, 1)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getLong(cursor.getColumnIndexOrThrow(Telephony.Sms._ID)) else -1L
    } ?: -1L

    fun messagesNewerThan(id: Long, limit: Int = 20): List<WireMessage> =
        query("${Telephony.Sms._ID} > ?", arrayOf(id.toString()), limit)?.use { cursor ->
            val cols = Columns(cursor)
            buildList { while (cursor.moveToNext()) add(cursor.toWireMessage(cols)) }
        }.orEmpty().reversed()

    /**
     * Best effort only. Writes to the SMS provider are reserved for the default SMS app, and a
     * non-default app's update is dropped without an error, so the Mac keeps its own read
     * marks and does not depend on this working. Logged so it is visible either way.
     */
    fun markThreadRead(threadId: Long): Int = runCatching {
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            android.content.ContentValues().apply { put(Telephony.Sms.READ, 1) },
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString()),
        )
    }.onSuccess { rows ->
        Log.i(TAG, "markThreadRead($threadId) updated $rows row(s) in the SMS provider")
    }.onFailure {
        Log.i(TAG, "markThreadRead($threadId) refused: ${it.message}")
    }.getOrDefault(0)

    /**
     * Ids of unread messages, grouped by thread.
     *
     * Deliberately not the shared projection: this only needs two id columns, and pulling
     * message bodies for every unread row just to count them is a lot of IPC for nothing.
     */
    private fun unreadCounts(watermarks: Map<Long, Long>): Map<Long, Int> {
        val idsByThread = HashMap<Long, MutableList<Long>>()
        // Inbox only: an outgoing row can carry read = 0 without meaning anything to the reader.
        val selection = "${Telephony.Sms.READ} = 0 AND ${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}"
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms._ID, Telephony.Sms.THREAD_ID),
            selection,
            null,
            "${Telephony.Sms.DATE} DESC LIMIT $UNREAD_SCAN_LIMIT",
        )?.use { cursor ->
            val threadIdCol = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            while (cursor.moveToNext()) {
                idsByThread.getOrPut(cursor.getLong(threadIdCol)) { mutableListOf() }
                    .add(cursor.getLong(idCol))
            }
        }
        return Unread.countsByThread(idsByThread, watermarks)
    }

    private fun query(selection: String?, args: Array<String>?, limit: Int): Cursor? =
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            PROJECTION,
            selection,
            args,
            "${Telephony.Sms.DATE} DESC LIMIT $limit",
        )

    private fun Cursor.toWireMessage(cols: Columns) = WireMessage(
        id = getLong(cols.id),
        threadId = getLong(cols.threadId),
        address = getString(cols.address).orEmpty(),
        body = getString(cols.body).orEmpty(),
        date = getLong(cols.date),
        outgoing = getInt(cols.type) == Telephony.Sms.MESSAGE_TYPE_SENT,
        read = getInt(cols.read) == 1,
    )

    private class Columns(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
        val threadId = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
        val address = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val body = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        val date = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
        val type = cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE)
        val read = cursor.getColumnIndexOrThrow(Telephony.Sms.READ)
    }

    private companion object {
        const val TAG = "SmsRepository"
        val PROJECTION = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
        )
        const val SNIPPET_CHARS = 140
        const val MESSAGES_PER_THREAD_SCAN = 8
        /**
         * A bound, not an expected size. At 500 a long-neglected inbox was undercounted —
         * only the newest 500 unread messages were considered, so the badge sat well below
         * the real number. Two id columns per row make a far larger cap cheap.
         */
        const val UNREAD_SCAN_LIMIT = 5000
    }
}
