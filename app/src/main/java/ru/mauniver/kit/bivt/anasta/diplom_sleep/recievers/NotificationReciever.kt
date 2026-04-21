package ru.mauniver.kit.bivt.anasta.diplom_sleep.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: return
        val text = intent.getStringExtra("text") ?: return
        val channelId = intent.getStringExtra("channelId") ?: return

        showNotification(context, title, text, channelId)
    }

    companion object {
        fun showNotification(context: Context, title: String, text: String, channelId: String) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName = when (channelId) {
                    "sleep_reminder" -> "Напоминания о сне"
                    "morning_survey" -> "Утренний опрос"
                    else -> "Уведомления"
                }
                val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Уведомления для напоминаний"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(channelId.hashCode(), notification)
        }
    }
}