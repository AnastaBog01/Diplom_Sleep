package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.android.gms.common.api.ApiException
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import ru.mauniver.kit.bivt.anasta.diplom_sleep.SleepTrackerApplication
import ru.mauniver.kit.bivt.anasta.diplom_sleep.managers.GoogleFitManager
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.WelcomeActivity
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.dialogs.AddSleepRecordDialog
import ru.mauniver.kit.bivt.anasta.diplom_sleep.utils.RecommendationEngine
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepEfficiency: TextView
    private lateinit var tvBedtime: TextView
    private lateinit var tvWakeTime: TextView
    private lateinit var llRecommendations: LinearLayout
    private lateinit var btnSyncData: Button
    private lateinit var cardAddRecord: CardView
    private lateinit var cardWeeklyAnalysis: CardView

    private lateinit var googleFitManager: GoogleFitManager
    private val handler = Handler(Looper.getMainLooper())

    private companion object {
        private const val REQUEST_FIT_PERMISSIONS = 1002
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

        googleFitManager = GoogleFitManager(requireActivity())
    }

    private fun initViews(view: View) {
        tvSleepDuration = view.findViewById(R.id.tvSleepDuration)
        tvSleepEfficiency = view.findViewById(R.id.tvSleepEfficiency)
        tvBedtime = view.findViewById(R.id.tvBedtime)
        tvWakeTime = view.findViewById(R.id.tvWakeTime)
        btnSyncData = view.findViewById(R.id.btnSyncData)
        llRecommendations = view.findViewById(R.id.llRecommendations)
        cardAddRecord = view.findViewById(R.id.cardAddRecord)
        cardWeeklyAnalysis = view.findViewById(R.id.cardWeeklyAnalysis)
    }

    private fun setupClickListeners() {
        btnSyncData.setOnClickListener {
            checkGoogleFitPermissions()
        }
        cardAddRecord.setOnClickListener {
            AddSleepRecordDialog.newInstance().show(parentFragmentManager, "AddSleepRecord")
        }
        cardWeeklyAnalysis.setOnClickListener {
            Toast.makeText(requireContext(), "Аналитика", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkGoogleFitPermissions() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null) {
            Toast.makeText(requireContext(), "Войдите в Google-аккаунт", Toast.LENGTH_SHORT).show()
            return
        }

        if (!googleFitManager.hasPermissions()) {
            googleFitManager.requestPermissions(REQUEST_FIT_PERMISSIONS)
        } else {
            syncGoogleFitSleep()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FIT_PERMISSIONS) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "Разрешения получены ✓", Toast.LENGTH_SHORT).show()
                syncGoogleFitSleep()
            } else {
                Toast.makeText(requireContext(), "Разрешения не предоставлены", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun syncGoogleFitSleep() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val sleepData = googleFitManager.getLastSleepSession()

                withContext(Dispatchers.Main) {
                    if (sleepData != null) {
                        val (startTime, endTime, durationMillis) = sleepData

                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

                        tvBedtime.text = sdf.format(Date(startTime))
                        tvWakeTime.text = sdf.format(Date(endTime))

                        val durationHours = durationMillis / (1000f * 60 * 60)
                        tvSleepDuration.text = String.format("%.1f ч", durationHours)

                        val efficiency = ((durationHours / 8f) * 100f).coerceIn(0f, 100f).toInt()
                        tvSleepEfficiency.text = "$efficiency%"

                        Toast.makeText(requireContext(), "✅ Данные сна загружены из Google Fit", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Не найдено данных о сне за последние дни", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("GoogleFit", "Sync failed", e)
                }
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
        lifecycleScope.launch(Dispatchers.Main) {
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


    override fun onResume() {
        super.onResume()
        loadRecommendations()
        loadLastSleepFromDb()
    }
}