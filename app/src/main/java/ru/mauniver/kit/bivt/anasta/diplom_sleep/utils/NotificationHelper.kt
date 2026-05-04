package ru.mauniver.kit.bivt.anasta.diplom_sleep.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import ru.mauniver.kit.bivt.anasta.diplom_sleep.receivers.NotificationReceiver
import java.util.Calendar

object NotificationHelper {

    fun scheduleRecommendationNotifications(context: Context) {
        scheduleDaily(context, "recommendation", 9, 0, "Совет дня", getRandomRecommendation())
        scheduleDaily(context, "recommendation", 21, 0, "Совет дня", getRandomRecommendation())
    }

    private fun scheduleDaily(
        context: Context,
        channelId: String,
        hour: Int,
        minute: Int,
        title: String,
        text: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
            putExtra("channelId", channelId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "$channelId$hour$minute".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun getRandomRecommendation(): String {
        val recommendations = listOf(
            "Избегайте кофеина после 15:00",
            "Заведите ритуал перед сном",
            "Поддерживайте температуру в комнате 18-20°C",
            "Ложитесь спать в одно и то же время",
            "Проветривайте комнату перед сном",
            "Откажитесь от гаджетов за час до сна",
            "Прогулка перед сном улучшает качество сна",
            "Не ешьте тяжелую пищу на ночь"
        )
        return recommendations.random()
    }
}