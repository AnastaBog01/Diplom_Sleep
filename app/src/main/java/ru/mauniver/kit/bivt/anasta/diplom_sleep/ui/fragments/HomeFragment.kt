package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.example.sleeptracker.R

class HomeFragment : Fragment() {

    private lateinit var tvSleepStart: TextView
    private lateinit var tvSleepEnd: TextView
    private lateinit var tvDebugLog: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        tvSleepStart = view.findViewById(R.id.tvSleepStart)
        tvSleepEnd = view.findViewById(R.id.tvSleepEnd)
        tvDebugLog = view.findViewById(R.id.tvDebugLog)  // нужен в разметке

        // Обновляем основной лог при создании
        updateLog()
        // Запускаем автообновление отладочного лога
        startUpdatingDebugLog()

        // Статистика сна
        val tvSleepDuration = view.findViewById<TextView>(R.id.tvSleepDuration)
        val tvSleepQuality = view.findViewById<TextView>(R.id.tvSleepQuality)
        val tvSleepEfficiency = view.findViewById<TextView>(R.id.tvSleepEfficiency)
        val tvBedtime = view.findViewById<TextView>(R.id.tvBedtime)

        tvSleepDuration.text = "7.5 ч"
        tvSleepQuality.text = "8.2"
        tvSleepEfficiency.text = "85%"
        tvBedtime.text = "22:30"

        // Карточки меню
        val cardAddRecord = view.findViewById<CardView>(R.id.cardAddRecord)
        val cardWeeklyAnalysis = view.findViewById<CardView>(R.id.cardWeeklyAnalysis)
        val cardSleepGoals = view.findViewById<CardView>(R.id.cardSleepGoals)
        val cardReport = view.findViewById<CardView>(R.id.cardReport)

        // Google Fit данные
        val tvSteps = view.findViewById<TextView>(R.id.tvSteps)
        val tvActivity = view.findViewById<TextView>(R.id.tvActivity)
        val btnSyncData = view.findViewById<Button>(R.id.btnSyncData)

        tvSteps.text = "8,542"
        tvActivity.text = "45 мин"

        btnSyncData.setOnClickListener {
            Toast.makeText(requireContext(), "Синхронизация с Google Fit прошла успешно", Toast.LENGTH_SHORT).show()
        }

        // Рекомендации
        val llRecommendations = view.findViewById<LinearLayout>(R.id.llRecommendations)

        val recommendations = listOf(
            "Избегайте кофеина после 15:00",
            "Заведите ритуал перед сном",
            "Поддерживайте температуру в комнате 18-20°C",
            "Занимайтесь спортом до 19:00"
        )

        for (recommendation in recommendations) {
            val textView = TextView(requireContext())
            textView.text = "• $recommendation"
            textView.textSize = 14f
            textView.setPadding(0, 12, 0, 12)
            textView.setTextColor(resources.getColor(android.R.color.black, null))
            llRecommendations.addView(textView)
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateLog()
        // Перезапускаем обновление отладочного лога (на случай, если было остановлено)
        startUpdatingDebugLog()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    private fun updateLog() {
        val prefs = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        val sleepStart = prefs.getString("last_sleep_start", "Заснул: —")
        val sleepEnd = prefs.getString("last_sleep_end", "Проснулся: —")
        tvSleepStart.text = sleepStart
        tvSleepEnd.text = sleepEnd
    }

    private fun startUpdatingDebugLog() {
        updateRunnable = object : Runnable {
            override fun run() {
                val prefs = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
                val debugText = prefs.getString("debug_log", "Ожидание данных...")
                tvDebugLog.text = debugText
                handler.postDelayed(this, 2000) // обновляем каждые 2 секунды
            }
        }
        handler.post(updateRunnable)
    }
}