package dev.haxzie.smsbridge.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.provider.Telephony
import android.util.Log
import dev.haxzie.smsbridge.net.WireMessage
import dev.haxzie.smsbridge.sms.SmsRepository

/**
 * Watches content://sms and reports rows that appear.
 *
 * This, not the SMS_RECEIVED broadcast, is the source of truth for new messages: the broadcast
 * fires *before* the default SMS app has written the row, so it carries no stable _id or
 * thread_id. Observing the provider also picks up messages sent from the phone itself.
 */
class SmsWatcher(
    private val context: Context,
    private val sms: SmsRepository,
    private val onNewMessages: (List<WireMessage>) -> Unit,
) {
    private var thread: HandlerThread? = null
    private var observer: ContentObserver? = null
    private var lastSeenId = -1L

    fun start() {
        if (observer != null) return
        lastSeenId = runCatching { sms.latestId() }.getOrDefault(-1L)
        val handlerThread = HandlerThread("sms-watcher").apply { start() }
        thread = handlerThread
        val contentObserver = object : ContentObserver(Handler(handlerThread.looper)) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = drain()
        }
        observer = contentObserver
        context.contentResolver.registerContentObserver(
            Telephony.Sms.CONTENT_URI, true, contentObserver,
        )
        Log.i(TAG, "watching from id $lastSeenId")
    }

    fun stop() {
        observer?.let { context.contentResolver.unregisterContentObserver(it) }
        observer = null
        thread?.quitSafely()
        thread = null
    }

    /** Also called straight from the SMS broadcast, which is only a wake-up hint. */
    fun drain() {
        val fresh = runCatching { sms.messagesNewerThan(lastSeenId) }
            .onFailure { Log.w(TAG, "drain failed: ${it.message}") }
            .getOrNull()
            ?.takeIf { it.isNotEmpty() }
            ?: return
        lastSeenId = fresh.maxOf { it.id }
        onNewMessages(fresh)
    }

    private companion object {
        const val TAG = "SmsWatcher"
    }
}
