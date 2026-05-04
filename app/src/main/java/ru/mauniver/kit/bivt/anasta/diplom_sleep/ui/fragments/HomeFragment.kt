package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import ru.mauniver.kit.bivt.anasta.diplom_sleep.SleepTrackerApplication
import ru.mauniver.kit.bivt.anasta.diplom_sleep.managers.HealthConnectManager
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.dialogs.AddSleepRecordDialog
import ru.mauniver.kit.bivt.anasta.diplom_sleep.utils.RecommendationEngine
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepEfficiency: TextView
    private lateinit var tvBedtime: TextView
    private lateinit var llRecommendations: LinearLayout
    private lateinit var btnSyncData: Button
    private lateinit var cardAddRecord: CardView
    private lateinit var cardWeeklyAnalysis: CardView
    private lateinit var tvWakeTime: TextView

    private lateinit var healthConnectManager: HealthConnectManager
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private val healthConnectPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            syncHealthConnectData()
        } else {
            Toast.makeText(requireContext(), "Разрешения не предоставлены", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadRecommendations()
        loadLastSleepFromDb()

        healthConnectManager = HealthConnectManager(requireContext())
    }

    private fun initViews(view: View) {
        tvSleepDuration = view.findViewById(R.id.tvSleepDuration)
        tvSleepEfficiency = view.findViewById(R.id.tvSleepEfficiency)
        tvBedtime = view.findViewById(R.id.tvBedtime)
        btnSyncData = view.findViewById(R.id.btnSyncData)
        llRecommendations = view.findViewById(R.id.llRecommendations)
        cardAddRecord = view.findViewById(R.id.cardAddRecord)
        cardWeeklyAnalysis = view.findViewById(R.id.cardWeeklyAnalysis)
        tvWakeTime = view.findViewById(R.id.tvWakeTime)
    }

    private fun setupClickListeners() {
        btnSyncData.setOnClickListener {
            checkHealthConnectPermissions()
        }
        cardAddRecord.setOnClickListener {
            AddSleepRecordDialog.newInstance().show(parentFragmentManager, "AddSleepRecord")
        }
        cardWeeklyAnalysis.setOnClickListener {
            Toast.makeText(requireContext(), "Аналитика", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkHealthConnectPermissions() {
        try {
            // Пробуем открыть Health Connect через стандартные настройки
            val intent = Intent("android.settings.HEALTH_CONNECT_SETTINGS")
            startActivity(intent)
        } catch (e: Exception) {
            // Если не открылся – пробуем через Play Market
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                startActivity(intent)
                Toast.makeText(requireContext(), "Откройте Health Connect и дайте разрешения приложению", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                // Если Play Market не открылся – показываем инструкцию
                showHealthConnectManualDialog()
            }
        }
    }

    private fun showHealthConnectManualDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Как найти Health Connect")
            .setMessage("1. Откройте Настройки → Приложения\n" +
                    "2. Нажмите на значок ⋮ (три точки) → Показать системные приложения\n" +
                    "3. Найдите Health Connect\n" +
                    "4. Откройте его и дайте разрешение приложению на чтение сна и шагов")
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun syncHealthConnectData() {
        lifecycleScope.launch {
            try {
                val steps = healthConnectManager.getStepsLast7Days()
                // если хотим показывать шаги, нужно добавить TextView
                // tvSteps.text = steps.toString()

                val sleep = healthConnectManager.getLastSleepSession()
                if (sleep != null) {
                    val startMillis = sleep.first.toEpochMilli()
                    val endMillis = sleep.second.toEpochMilli()
                    val durationHours = (endMillis - startMillis) / (1000f * 60 * 60)
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    tvBedtime.text = sdf.format(Date(startMillis))
                    tvWakeTime.text = sdf.format(Date(endMillis))
                    tvSleepDuration.text = String.format("%.1f ч", durationHours)

                    val efficiency = (durationHours / 8f * 100f).toInt().coerceIn(0, 100)
                    tvSleepEfficiency.text = "$efficiency%"

                    Toast.makeText(requireContext(), "Данные синхронизированы", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Нет данных о сне в Health Connect", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadLastSleepFromDb() {
        lifecycleScope.launch {
            if (!isAdded) return@launch
            val app = requireContext().applicationContext as SleepTrackerApplication
            val records = app.repository.getAllRecords()
            if (records.isNotEmpty()) {
                val lastRecord = records.maxByOrNull { it.endTime }
                if (lastRecord != null) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    tvBedtime.text = sdf.format(Date(lastRecord.startTime))
                    tvWakeTime.text = sdf.format(Date(lastRecord.endTime))

                    val durationHours = (lastRecord.endTime - lastRecord.startTime) / (1000f * 60 * 60)
                    tvSleepDuration.text = String.format("%.1f ч", durationHours)

                    val efficiency = (durationHours / 8f * 100f).toInt()
                    tvSleepEfficiency.text = "${efficiency.coerceIn(0, 100)}%"
                }
            } else {
                tvBedtime.text = "—"
                tvWakeTime.text = "—"
                tvSleepDuration.text = "—"
                tvSleepEfficiency.text = "—"
            }
        }
    }

    private fun loadRecommendations() {
        lifecycleScope.launch {
            if (!isAdded) return@launch
            val app = requireContext().applicationContext as SleepTrackerApplication
            val records = app.repository.getAllRecords()
            val recommendations = RecommendationEngine.generateRecommendations(records)

            llRecommendations.removeAllViews()
            if (recommendations.isEmpty()) {
                val tv = TextView(requireContext())
                tv.text = "Добавьте 2+ записи с факторами и оценкой."
                tv.textSize = 14f
                tv.setPadding(0, 12, 0, 12)
                llRecommendations.addView(tv)
            } else {
                for (rec in recommendations) {
                    val tv = TextView(requireContext())
                    tv.text = "• $rec"
                    tv.textSize = 14f
                    tv.setPadding(0, 12, 0, 12)
                    tv.setTextColor(resources.getColor(android.R.color.black, null))
                    llRecommendations.addView(tv)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onResume() {
        super.onResume()
        loadRecommendations()
        loadLastSleepFromDb()
    }
}