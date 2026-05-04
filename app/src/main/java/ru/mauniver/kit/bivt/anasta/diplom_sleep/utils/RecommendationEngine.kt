package ru.mauniver.kit.bivt.anasta.diplom_sleep.utils

import android.content.Context
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord

object RecommendationEngine {

    fun generateRecommendations(records: List<SleepRecord>): List<String> {
        if (records.size < 2) return getDefaultRecommendations()

        val allFactors = extractAllFactors(records)
        val recommendations = mutableListOf<String>()

        for (factor in allFactors) {
            val qualityWith = mutableListOf<Int>()
            val qualityWithout = mutableListOf<Int>()

            for (record in records) {
                val factors = record.factors?.split(",")?.map { it.trim() } ?: emptyList()
                val quality = record.quality
                if (quality != null && quality > 0) {
                    if (factors.contains(factor)) {
                        qualityWith.add(quality)
                    } else {
                        qualityWithout.add(quality)
                    }
                }
            }

            if (qualityWith.isNotEmpty() && qualityWithout.isNotEmpty()) {
                val avgWith = qualityWith.average()
                val avgWithout = qualityWithout.average()
                val diff = avgWithout - avgWith

                if (diff > 1.5) {
                    val percent = ((diff / 10) * 100).toInt()
                    recommendations.add("$factor ухудшает ваш сон в среднем на $percent%")
                } else if (diff < -1.5) {
                    val percent = (((-diff) / 10) * 100).toInt()
                    recommendations.add("$factor улучшает ваш сон в среднем на $percent%")
                }
            }
        }

        return if (recommendations.isNotEmpty()) recommendations else getDefaultRecommendations()
    }

    private fun extractAllFactors(records: List<SleepRecord>): Set<String> {
        val factors = mutableSetOf<String>()
        for (record in records) {
            record.factors?.split(",")?.map { it.trim() }?.let { factors.addAll(it) }
        }
        return factors
    }

    private fun getDefaultRecommendations(): List<String> {
        return listOf(
            "Избегайте кофеина после 15:00",
            "Заведите ритуал перед сном",
            "Поддерживайте температуру в комнате 18-20°C"
        )
    }
}