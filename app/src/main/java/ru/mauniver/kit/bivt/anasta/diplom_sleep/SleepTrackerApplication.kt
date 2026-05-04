package ru.mauniver.kit.bivt.anasta.diplom_sleep

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepDatabase
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRepository
import ru.mauniver.kit.bivt.anasta.diplom_sleep.utils.NotificationHelper

class SleepTrackerApplication : Application() {

    lateinit var repository: SleepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Инициализация базы данных и репозитория
        val database = SleepDatabase.getInstance(this)
        repository = SleepRepository(database.sleepDao())

        // Запланировать уведомления для рекомендаций (два раза в день)
        NotificationHelper.scheduleRecommendationNotifications(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val sleepChannel = NotificationChannel(
                "sleep_reminder",
                "Напоминания о сне",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о времени сна"
            }

            val surveyChannel = NotificationChannel(
                "morning_survey",
                "Утренний опрос",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Утренний опрос о качестве сна"
            }

            val recommendationChannel = NotificationChannel(
                "recommendation",
                "Рекомендации",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Ежедневные рекомендации по улучшению сна"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(sleepChannel)
            notificationManager.createNotificationChannel(surveyChannel)
            notificationManager.createNotificationChannel(recommendationChannel)
        }
    }
}