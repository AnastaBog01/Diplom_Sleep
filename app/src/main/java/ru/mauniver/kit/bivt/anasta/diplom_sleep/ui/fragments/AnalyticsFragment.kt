package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.sleeptracker.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import ru.mauniver.kit.bivt.anasta.diplom_sleep.SleepTrackerApplication
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.viewmodels.AnalyticsViewModel
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.*

class AnalyticsFragment : Fragment() {

    private lateinit var chartQuality: LineChart
    private lateinit var chartDuration: BarChart
    private lateinit var chartPhases: PieChart
    private lateinit var tvDebugAnalytics: TextView
    private lateinit var tvAverageQuality: TextView
    private lateinit var tvAverageDuration: TextView
    private lateinit var tvTotalTime: TextView
    private lateinit var tvEfficiency: TextView
//    private lateinit var tvWakeups: TextView
    private lateinit var viewModel: AnalyticsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_analytics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chartQuality = view.findViewById(R.id.chartQuality)
        chartDuration = view.findViewById(R.id.chartDuration)
        chartPhases = view.findViewById(R.id.chartPhases)
        tvDebugAnalytics = view.findViewById(R.id.tvDebugAnalytics)
        tvAverageQuality = view.findViewById(R.id.tvAverageQuality)
        tvAverageDuration = view.findViewById(R.id.tvAverageDuration)
        tvTotalTime = view.findViewById(R.id.tvTotalTime)
        tvEfficiency = view.findViewById(R.id.tvEfficiency)
//        tvWakeups = view.findViewById(R.id.tvWakeups)

//        setupPhasesChart()

        val app = requireContext().applicationContext as SleepTrackerApplication
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(app.repository) as T
            }
        }).get(AnalyticsViewModel::class.java)

        viewModel.records.observe(viewLifecycleOwner) { records ->
            val debugText = buildString {
                appendLine("Записей в БД: ${records.size}")
                if (records.isNotEmpty()) {
                    val sdf = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
                    // Сортируем от новых к старым
                    val sorted = records.sortedByDescending { it.startTime }
                    val latest = sorted.first()
                    appendLine("Последняя: ${sdf.format(Date(latest.startTime))} - ${sdf.format(Date(latest.endTime))}")
                    // Добавляем список уникальных дат (по времени засыпания или пробуждения)
                    val dates = records.map { sdf.format(Date(it.startTime)) }.distinct()
                    appendLine("Даты записей: ${dates.joinToString()}")
                    val sdfDate = SimpleDateFormat("dd.MM", Locale.getDefault())
                    sdfDate.timeZone = TimeZone.getTimeZone("UTC")
                    val dates1 = records.map { sdfDate.format(Date(it.endTime)) }.distinct()
                    appendLine("Даты записей (UTC): ${dates1.joinToString()}")
                }
                appendLine("Средняя продолжительность за неделю: ${String.format("%.1f", calculateAverageDuration(records))} ч")
            }
            tvDebugAnalytics.text = debugText

            if (records.isNotEmpty()) {
                updateChartsWithRealData(records)
            }
        }
        viewModel.loadRecordsForLastDays()
    }

    private fun setupPhasesChart() {
        val phaseNames = listOf("Глубокий сон", "REM-сон", "Легкий сон", "Бодрствование")
        val phasePercentages = listOf(25f, 20f, 45f, 10f)
        val entries = phasePercentages.mapIndexed { index, p -> PieEntry(p, phaseNames[index]) }
        val colors = listOf(
            Color.parseColor("#6750A4"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#CE93D8"),
            Color.parseColor("#E1BEE7")
        )
        val dataSet = PieDataSet(entries, "Фазы сна").apply {
            this.colors = colors
            valueTextSize = 12f
            setDrawIcons(false)
            sliceSpace = 3f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }
        chartPhases.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(40f)
            setTransparentCircleRadius(45f)
            setDrawEntryLabels(true)
            setExtraOffsets(5f, 5f, 5f, 5f)
            legend.isEnabled = true
            invalidate()
        }
    }

    private fun updateChartsWithRealData(records: List<SleepRecord>) {
        // Используем UTC календарь
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val days = mutableListOf<String>()
        val durationValues = mutableListOf<Float>()

        for (i in 6 downTo 0) {
            utcCalendar.time = Date()
            utcCalendar.add(Calendar.DAY_OF_YEAR, -i)

            // Название дня недели (можно оставить как есть)
            val dayOfWeek = when (utcCalendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Пн"
                Calendar.TUESDAY -> "Вт"
                Calendar.WEDNESDAY -> "Ср"
                Calendar.THURSDAY -> "Чт"
                Calendar.FRIDAY -> "Пт"
                Calendar.SATURDAY -> "Сб"
                Calendar.SUNDAY -> "Вс"
                else -> ""


            }
            days.add(dayOfWeek)

            // Начало дня в UTC (00:00:00)
            utcCalendar.set(Calendar.HOUR_OF_DAY, 0)
            utcCalendar.set(Calendar.MINUTE, 0)
            utcCalendar.set(Calendar.SECOND, 0)
            utcCalendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = utcCalendar.timeInMillis

            // Конец дня (23:59:59.999)
            utcCalendar.add(Calendar.DAY_OF_YEAR, 1)
            val endOfDay = utcCalendar.timeInMillis - 1
            utcCalendar.add(Calendar.DAY_OF_YEAR, -1) // вернули обратно

            val recordsForDay = records.filter { it.endTime in startOfDay..endOfDay }
            var totalHours = 0f
            for (record in recordsForDay) {
                totalHours += (record.endTime - record.startTime) / (1000f * 60 * 60)
            }
            durationValues.add(totalHours)
        }

        // Столбчатый график
        val barEntries = durationValues.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }
        val barDataSet = BarDataSet(barEntries, "Продолжительность сна (ч)").apply {
            color = getColor(R.color.chart_purple)
            valueTextSize = 10f
            setDrawValues(true)
        }
        chartDuration.apply {
            data = BarData(barDataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(days)
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = (durationValues.maxOrNull() ?: 8f) + 1f
            invalidate()
        }

        // Линейный график
        val lineEntries = durationValues.mapIndexed { index, value -> Entry(index.toFloat(), value) }
        val lineDataSet = LineDataSet(lineEntries, "Продолжительность сна (ч)").apply {
            color = getColor(R.color.chart_purple)
            setCircleColor(getColor(R.color.chart_purple))
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            valueTextSize = 10f
            setDrawFilled(true)
            fillColor = getColor(R.color.chart_purple)
            fillAlpha = 50
        }
        chartQuality.apply {
            data = LineData(lineDataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(days)
            axisLeft.axisMinimum = 0f
            axisLeft.axisMaximum = (durationValues.maxOrNull() ?: 8f) + 1f
            invalidate()
        }

        val avgDuration = if (durationValues.isNotEmpty()) durationValues.average() else 0.0
        tvAverageDuration.text = "Среднее: ${String.format("%.1f", avgDuration)} ч"
        tvAverageQuality.text = "Среднее: ${String.format("%.1f", avgDuration)}/10"

        val totalSleepHours = durationValues.sum()
        tvTotalTime.text = String.format("%.1f", totalSleepHours)
        tvEfficiency.text = if (totalSleepHours > 0) "85" else "0"
//        tvWakeups.text = "2"

        // Вычисляем среднюю оценку самочувствия (quality)
        val feelings = records.mapNotNull { it.quality } // quality = feeling
        val avgFeeling = if (feelings.isNotEmpty()) feelings.average() else 0.0
        tvEfficiency.text = if (avgFeeling > 0) String.format("%.0f", avgFeeling) else "—"
//        tvWakeups.text = records.size.toString() // количество ночей за период

        // Добавляем расчёт фаз сна для последней записи
        val latestRecord = records.maxByOrNull { it.endTime }
        if (latestRecord != null) {
            val durationHours = (latestRecord.endTime - latestRecord.startTime) / (1000f * 60 * 60)
            val phases = calculateSleepPhases(durationHours) // метод, который ты добавила
            updatePhasesChart(phases) // обновляем круговую диаграмму

            // Обновляем карточки для последнего сна
            tvTotalTime.text = String.format("%.1f", durationHours) // часы
            val feeling = latestRecord.quality ?: 0
            tvEfficiency.text = feeling.toString() // оценка самочувствия (1-10)
            val deepPercent = phases["Глубокий сон"] ?: 0f
//            tvWakeups.text = "${deepPercent.toInt()}%" // процент глубокого сна
        } else {
            // Если нет записей – показываем прочерки
            tvTotalTime.text = "—"
            tvEfficiency.text = "—"
//            tvWakeups.text = "—"
            updatePhasesChart(mapOf("Глубокий сон" to 0f, "REM-сон" to 0f, "Легкий сон" to 0f))
        }
    }

    private fun calculateAverageDuration(records: List<SleepRecord>): Double {
        if (records.isEmpty()) return 0.0
        var totalHours = 0.0
        for (record in records) {
            totalHours += (record.endTime - record.startTime) / (1000f * 60 * 60)
        }
        return totalHours / records.size
    }

    private fun getColor(colorResId: Int): Int {
        return androidx.core.content.ContextCompat.getColor(requireContext(), colorResId)
    }
    private fun calculateSleepPhases(durationHours: Float): Map<String, Float> {
        return when {
            durationHours < 6 -> mapOf("Глубокий сон" to 15f, "REM-сон" to 15f, "Легкий сон" to 70f)
            durationHours in 6.0..8.0 -> mapOf("Глубокий сон" to 20f, "REM-сон" to 20f, "Легкий сон" to 60f)
            else -> mapOf("Глубокий сон" to 25f, "REM-сон" to 25f, "Легкий сон" to 50f)
        }
    }

    private fun updatePhasesChart(phases: Map<String, Float>) {
        val entries = phases.map { PieEntry(it.value, it.key) }
        val colors = listOf(
            Color.parseColor("#6750A4"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#CE93D8")
        )
        val dataSet = PieDataSet(entries, "Фазы сна").apply {
            this.colors = colors
            valueTextSize = 12f
            setDrawIcons(false)
            sliceSpace = 3f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return "${value.toInt()}%"
                }
            }
        }
        chartPhases.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawHoleEnabled(true)
            setHoleRadius(40f)
            setTransparentCircleRadius(45f)
            setDrawEntryLabels(true)
            setExtraOffsets(5f, 5f, 5f, 5f)
            legend.isEnabled = true
            invalidate()
        }
    }
}