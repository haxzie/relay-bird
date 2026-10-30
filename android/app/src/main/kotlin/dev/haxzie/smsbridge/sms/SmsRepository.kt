package dev.haxzie.smsbridge.sms

import android.content.Context
import android.database.Cursor
import android.provider.Telephony
import dev.haxzie.smsbridge.net.WireMessage
import dev.haxzie.smsbridge.net.WireThread

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

    fun threads(limit: Int = 50): List<WireThread> {
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
                )
            }
        }
        val unread = unreadCounts()
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

    fun markThreadRead(threadId: Long): Int = runCatching {
        context.contentResolver.update(
            Telephony.Sms.CONTENT_URI,
            android.content.ContentValues().apply { put(Telephony.Sms.READ, 1) },
            "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
            arrayOf(threadId.toString()),
        )
    }.getOrDefault(0)

    private fun unreadCounts(): Map<Long, Int> {
        val counts = HashMap<Long, Int>()
        query("${Telephony.Sms.READ} = 0", null, UNREAD_SCAN_LIMIT)?.use { cursor ->
            val threadIdCol = cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(threadIdCol)
                counts[id] = (counts[id] ?: 0) + 1
            }
        }
        return counts
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
        const val UNREAD_SCAN_LIMIT = 500
    }
}
