package dev.haxzie.smsbridge.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import dev.haxzie.smsbridge.SmsBridgeApp

/**
 * A wake-up hint only — the row is read back from the provider, which is where stable ids and
 * thread ids come from (see [SmsWatcher]).
 *
 * SMS_RECEIVED is on the Android 8+ implicit-broadcast exemption list, so a manifest-declared
 * receiver still fires with the app in the background.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val container = (context.applicationContext as SmsBridgeApp).container
        if (!container.running.value) {
            // Android 12+ forbids starting a foreground service from the background, so the
            // message cannot be forwarded now. Tell the user rather than failing silently.
            Notifications.ensureChannels(context)
            Notifications.resumePrompt(context)
            return
        }

        // Logged only — long messages arrive as several parts in one broadcast, and the
        // provider has already reassembled them by the time the observer fires.
        val parts = runCatching { Telephony.Sms.Intents.getMessagesFromIntent(intent) }
            .getOrNull()
            ?.size ?: 0
        Log.i(TAG, "SMS broadcast, $parts part(s)")
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}
