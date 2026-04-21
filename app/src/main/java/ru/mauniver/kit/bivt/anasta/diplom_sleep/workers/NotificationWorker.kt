package ru.mauniver.kit.bivt.anasta.diplom_sleep.workers

//import android.content.Context
//import androidx.work.Worker
//import androidx.work.WorkerParameters
//import ru.mauniver.kit.bivt.anasta.diplom_sleep.receivers.NotificationReceiver
//
//class NotificationWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
//    override fun doWork(): Result {
//        val title = inputData.getString("title") ?: return Result.failure()
//        val text = inputData.getString("text") ?: return Result.failure()
//        val channelId = inputData.getString("channelId") ?: return Result.failure()
//
//        // Вызываем статический метод showNotification (должен быть в companion object)
//        NotificationReceiver.showNotification(applicationContext, title, text, channelId)
//        return Result.success()
//    }
//}