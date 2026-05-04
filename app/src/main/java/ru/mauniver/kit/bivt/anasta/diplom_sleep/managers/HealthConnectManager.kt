package ru.mauniver.kit.bivt.anasta.diplom_sleep.managers

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

class HealthConnectManager(private val context: Context) {

    private fun getClient(): HealthConnectClient? {
        return try {
            if (HealthConnectClient.getSdkStatus(context) ==
                HealthConnectClient.SDK_AVAILABLE
            ) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getLastSleepSession(): Pair<Instant, Instant>? {
        val client = getClient()
        if (client == null) {
            android.util.Log.e("HealthConnect", "Health Connect клиент недоступен")
            return null
        }

        val end = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)

        android.util.Log.d("HealthConnect", "Запрашиваем сон с $start по $end")

        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )

        return try {
            val response = client.readRecords(request)
            android.util.Log.d("HealthConnect", "Найдено записей: ${response.records.size}")
            val latest = response.records.maxByOrNull { it.endTime }
            android.util.Log.d("HealthConnect", "Последний сон: $latest")
            latest?.let { it.startTime to it.endTime }
        } catch (e: Exception) {
            android.util.Log.e("HealthConnect", "Ошибка чтения сна: ${e.message}")
            null
        }
    }

    suspend fun getStepsLast7Days(): Long {
        val client = getClient() ?: return 0L

        val end = Instant.now()
        val start = end.minus(7, ChronoUnit.DAYS)

        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )

        return try {
            client.readRecords(request)
                .records
                .sumOf { it.count }
        } catch (e: Exception) {
            0L
        }

    }

}