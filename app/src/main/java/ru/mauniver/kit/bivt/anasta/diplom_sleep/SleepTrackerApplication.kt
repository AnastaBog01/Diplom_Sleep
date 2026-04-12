package ru.mauniver.kit.bivt.anasta.diplom_sleep
//Класс приложения, который создается при первом запуске. Бд и репозиторий в 1 экземпляре, живут столько сколько и приложение
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepDatabase
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRepository

class SleepTrackerApplication : Application() {

    lateinit var repository: SleepRepository
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Инициализация базы данных и репозитория
        val database = SleepDatabase.getInstance(this)
        repository = SleepRepository(database.sleepDao())
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sleep_reminder",
                "Напоминания о сне",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Напоминания о времени сна"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}