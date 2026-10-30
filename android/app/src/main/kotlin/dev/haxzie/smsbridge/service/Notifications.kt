package dev.haxzie.smsbridge.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.haxzie.smsbridge.MainActivity
import dev.haxzie.smsbridge.R

object Notifications {
    const val CHANNEL_STATUS = "bridge_status"
    const val CHANNEL_ALERTS = "bridge_alerts"
    const val ID_FOREGROUND = 1
    const val ID_RESUME_PROMPT = 2

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_STATUS, "Mirroring status", NotificationManager.IMPORTANCE_LOW).apply {
                description = "The ongoing notification Android requires while mirroring is active."
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when mirroring has stopped and a message was missed."
            },
        )
    }

    fun foreground(context: Context, text: String): Notification =
        NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SMS Bridge")
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setOngoing(true)
            .setShowWhen(false)
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
