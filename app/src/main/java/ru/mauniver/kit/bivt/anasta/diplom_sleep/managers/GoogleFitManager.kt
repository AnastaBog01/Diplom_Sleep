package ru.mauniver.kit.bivt.anasta.diplom_sleep.managers

import android.app.Activity
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class GoogleFitManager(private val activity: Activity) {

    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
        .build()

    fun hasPermissions(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return false
        return GoogleSignIn.hasPermissions(account, fitnessOptions)
    }

    fun requestPermissions(requestCode: Int) {
        val account = GoogleSignIn.getLastSignedInAccount(activity) ?: return
        GoogleSignIn.requestPermissions(activity, requestCode, account, fitnessOptions)
    }

    suspend fun getLastSleepSession(): Triple<Long, Long, Long>? =
        withContext(Dispatchers.IO) {

            val account = GoogleSignIn.getLastSignedInAccount(activity)
                ?: return@withContext null

            val endTime = System.currentTimeMillis()
            val startTime = endTime - 30L * 24 * 60 * 60 * 1000 // 30 дней

            val request = com.google.android.gms.fitness.request.SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .build()

            try {
                val response = Tasks.await(
                    Fitness.getSessionsClient(activity, account)
                        .readSession(request)
                )

                if (response.sessions.isEmpty()) return@withContext null

                // Берём самую позднюю сессию с ненулевой длительностью
                val latest = response.sessions
                    .filter {
                        it.getEndTime(TimeUnit.MILLISECONDS) >
                                it.getStartTime(TimeUnit.MILLISECONDS)
                    }
                    .maxByOrNull {
                        it.getEndTime(TimeUnit.MILLISECONDS)
                    }
                    ?: return@withContext null

                val start = latest.getStartTime(TimeUnit.MILLISECONDS)
                val end = latest.getEndTime(TimeUnit.MILLISECONDS)
                val duration = end - start

                // Отсекаем мусор (меньше 2 часов)
                if (duration < 2 * 60 * 60 * 1000L) return@withContext null

                Triple(start, end, duration)

            } catch (e: Exception) {
                Log.e("GoogleFit", "Ошибка чтения сна", e)
                null
            }
        }
}