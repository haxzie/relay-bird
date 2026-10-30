package dev.haxzie.relaybird.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.haxzie.relaybird.MainActivity
import dev.haxzie.relaybird.R

object Notifications {
    // A channel's importance and badge setting are fixed once created — only the user can
    // change them afterwards — so altering either means publishing a new channel id.
    const val CHANNEL_STATUS = "bridge_status_v2"
    private const val CHANNEL_STATUS_LEGACY = "bridge_status"
    const val CHANNEL_ALERTS = "bridge_alerts"
    const val ID_FOREGROUND = 1
    const val ID_RESUME_PROMPT = 2

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(CHANNEL_STATUS_LEGACY)
        manager.createNotificationChannel(
            // IMPORTANCE_LOW, not MIN. Android's own guidance is that MIN must not be used with
            // startForeground, and that doing so makes the system post its own, higher-priority
            // "running in the background" notice instead — worse than the one being hidden.
            NotificationChannel(CHANNEL_STATUS, "Mirroring status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "The ongoing notification Android requires while mirroring is active. " +
                    "On Android 14 and later you can swipe it away; mirroring keeps running."
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
                setSound(null, null)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when mirroring has stopped and a message was missed."
            },
        )
    }

    fun foreground(context: Context): Notification =
        NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("RelayBird")
            .setContentText("Mirroring your messages")
            .setContentIntent(openApp(context))
            // Not setOngoing: since Android 14 that no longer blocks dismissal, and being
            // dismissible is the point.
            .setShowWhen(false)
            .setSilent(true)
            // Keeps it out of the lock screen, where it would otherwise sit permanently.
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    /**
     * Android 12+ forbids starting a foreground service from the background, so when an SMS
     * arrives with mirroring stopped the only honest thing to do is ask the user to resume it.
     */
    fun resumePrompt(context: Context) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mirroring is off")
            .setContentText("A message arrived but your Macs are not connected. Tap to resume.")
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(ID_RESUME_PROMPT, notification)
    }

    private fun openApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
