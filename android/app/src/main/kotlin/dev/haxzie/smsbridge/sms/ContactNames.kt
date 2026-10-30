package dev.haxzie.smsbridge.sms

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.util.Log
import java.util.Locale

/**
 * Phone number -> display name.
 *
 * Built from a single bulk query over the Phone data table rather than a PhoneLookup per
 * address: the thread list touches every distinct sender, and PhoneLookup would make that
 * hundreds of IPC round-trips.
 */
class ContactNames(private val context: Context) {
    private var byNumber: Map<String, String>? = null
    private var observer: ContentObserver? = null

    fun nameFor(address: String): String? {
        val map = byNumber ?: load().also { byNumber = it }
        val key = normalize(address) ?: return null
        return map[key]
    }

    fun startWatching() {
        if (observer != null) return
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                byNumber = null
            }
        }.also {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI, true, it,
            )
        }
    }

    fun stopWatching() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
    }

    private fun load(): Map<String, String> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        )
        val out = HashMap<String, String>()
        try {
            query(context.contentResolver, projection)?.use { cursor ->
                val numberCol = cursor.getColumnIndexOrThrow(projection[0])
                val normalizedCol = cursor.getColumnIndexOrThrow(projection[1])
                val nameCol = cursor.getColumnIndexOrThrow(projection[2])
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameCol) ?: continue
                    val normalized = cursor.getString(normalizedCol)
                    val raw = cursor.getString(numberCol)
                    (normalize(normalized) ?: normalize(raw))?.let { out.putIfAbsent(it, name) }
                }
            }
        } catch (e: SecurityException) {
            // READ_CONTACTS not granted; addresses just render as numbers.
            Log.i(TAG, "contacts unavailable: ${e.message}")
        }
        return out
    }

    private fun query(resolver: ContentResolver, projection: Array<String>) = resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null,
    )

    /**
     * Matching on the last [MATCH_DIGITS] digits: SMS addresses and contact entries routinely
     * disagree about country code and formatting, and a strict E.164 comparison would miss
     * most of them.
     */
    private fun normalize(number: String?): String? {
        if (number.isNullOrBlank()) return null
        val e164 = PhoneNumberUtils.formatNumberToE164(number, Locale.getDefault().country.uppercase())
        val digits = (e164 ?: number).filter { it.isDigit() }
        if (digits.isEmpty()) {
            // Short codes and alphanumeric senders ("AMZN") have no digits at all.
            return number.trim().uppercase()
        }
        return digits.takeLast(MATCH_DIGITS)
    }

    private companion object {
        const val TAG = "ContactNames"
        const val MATCH_DIGITS = 9
    }
}
