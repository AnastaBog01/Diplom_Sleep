package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
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
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.dialogs.AddSleepRecordDialog
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    // UI элементы
    private lateinit var tvSleepDuration: TextView
    private lateinit var tvSleepQuality: TextView
    private lateinit var tvSleepEfficiency: TextView
    private lateinit var tvBedtime: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvActivity: TextView
    private lateinit var btnSyncData: Button
    private lateinit var llRecommendations: LinearLayout
    private lateinit var cardAddRecord: CardView
    private lateinit var cardWeeklyAnalysis: CardView
    private lateinit var cardSleepGoals: CardView
    private lateinit var cardReport: CardView

    // Логи сна
    private lateinit var tvSleepStart: TextView
    private lateinit var tvSleepEnd: TextView
    private lateinit var tvDebugLog: TextView
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    // Google Fit
    private val fitnessOptions = FitnessOptions.builder()
        .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
        .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners()
        loadRecommendations()

        // Логи сна
        updateSleepLog()
        startUpdatingDebugLog()

        // Google Fit
        //checkPermissionsAndSync()
    }

    private fun initViews(view: View) {
        tvSleepDuration = view.findViewById(R.id.tvSleepDuration)
        tvSleepQuality = view.findViewById(R.id.tvSleepQuality)
        tvSleepEfficiency = view.findViewById(R.id.tvSleepEfficiency)
        tvBedtime = view.findViewById(R.id.tvBedtime)
        tvSteps = view.findViewById(R.id.tvSteps)
        tvActivity = view.findViewById(R.id.tvActivity)
        btnSyncData = view.findViewById(R.id.btnSyncData)
        llRecommendations = view.findViewById(R.id.llRecommendations)
        cardAddRecord = view.findViewById(R.id.cardAddRecord)
        cardWeeklyAnalysis = view.findViewById(R.id.cardWeeklyAnalysis)
        cardSleepGoals = view.findViewById(R.id.cardSleepGoals)
        cardReport = view.findViewById(R.id.cardReport)
        tvSleepStart = view.findViewById(R.id.tvSleepStart)
        tvSleepEnd = view.findViewById(R.id.tvSleepEnd)
        tvDebugLog = view.findViewById(R.id.tvDebugLog)
    }

    private fun setupClickListeners() {
        btnSyncData.setOnClickListener {
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isGuest = prefs.getBoolean("guest_mode", false)
            if (isGuest) {
                Toast.makeText(requireContext(), "Синхронизация с Google Fit недоступна", Toast.LENGTH_SHORT).show()
                tvSteps.text = "—"
                tvActivity.text = "—"
            } else {
                checkPermissionsAndSync()
            }
        }
        cardAddRecord.setOnClickListener {
            val dialog = AddSleepRecordDialog.newInstance()
            dialog.show(parentFragmentManager, "AddSleepRecord")}
        cardWeeklyAnalysis.setOnClickListener { Toast.makeText(requireContext(), "Аналитика", Toast.LENGTH_SHORT).show() }
        cardSleepGoals.setOnClickListener { Toast.makeText(requireContext(), "Цели сна (в разработке)", Toast.LENGTH_SHORT).show() }
        cardReport.setOnClickListener { Toast.makeText(requireContext(), "Отчёт (в разработке)", Toast.LENGTH_SHORT).show() }
    }

    private fun loadRecommendations() {
        val recommendations = listOf(
            "Избегайте кофеина после 15:00",
            "Заведите ритуал перед сном",
            "Поддерживайте температуру в комнате 18-20°C",
            "Занимайтесь спортом до 19:00"
        )
        for (rec in recommendations) {
            val textView = TextView(requireContext())
            textView.text = "• $rec"
            textView.textSize = 14f
            textView.setPadding(0, 12, 0, 12)
            textView.setTextColor(resources.getColor(android.R.color.black, null))
            llRecommendations.addView(textView)
        }
    }

    //Логи сна (SharedPreferences)

    private fun updateSleepLog() {
        if (!isAdded) return
        val prefs = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
        val sleepStart = prefs.getString("last_sleep_start", "Заснул: —")
        val sleepEnd = prefs.getString("last_sleep_end", "Проснулся: —")
        tvSleepStart.text = sleepStart
        tvSleepEnd.text = sleepEnd
    }

    private fun startUpdatingDebugLog() {
        updateRunnable = object : Runnable {
            override fun run() {
                if (!isAdded) return
                val prefs = requireContext().getSharedPreferences("sleep_data", Context.MODE_PRIVATE)
                val debugText = prefs.getString("debug_log", "Ожидание данных...")
                tvDebugLog.text = debugText
                handler.postDelayed(this, 2000)
            }
        }
        handler.post(updateRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateSleepLog()
    }

    //Google Fit

    private fun checkPermissionsAndSync() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account == null) {
            // Не показываем тост, если включён гостевой режим
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isGuest = prefs.getBoolean("guest_mode", false)
            if (!isGuest) {
                Toast.makeText(requireContext(), "Сначала войдите в Google аккаунт", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                this,
                REQUEST_FIT_PERMISSIONS,
                account,
                fitnessOptions
            )
        } else {
            syncGoogleFitData()
        }
    }

    private fun syncGoogleFitData() {
        lifecycleScope.launch {
            try {
                val steps = getSteps()
                if (steps != null) tvSteps.text = steps.toString() else tvSteps.text = "—"
                val activityMinutes = getActivityMinutes()
                if (activityMinutes != null) tvActivity.text = "${activityMinutes} мин" else tvActivity.text = "—"
                Toast.makeText(requireContext(), "Синхронизация завершена", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun getSteps(): Long? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext()) ?: return@withContext null
        val response = Tasks.await(
            Fitness.getHistoryClient(requireContext(), account)
                .readDailyTotal(DataType.TYPE_STEP_COUNT_DELTA)
        )
        return@withContext response?.dataPoints?.firstOrNull()
            ?.getValue(Field.FIELD_STEPS)?.asInt()?.toLong()
    }

    private suspend fun getActivityMinutes(): Int? = withContext(Dispatchers.IO) {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext()) ?: return@withContext null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.DAYS.toMillis(7)

        val request = DataReadRequest.Builder()
            .read(DataType.TYPE_ACTIVITY_SEGMENT)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()

        val response = Tasks.await(
            Fitness.getHistoryClient(requireContext(), account).readData(request)
        )

        var totalMinutes = 0
        for (bucket in response.buckets) {
            for (dataSet in bucket.dataSets) {
                for (dataPoint in dataSet.dataPoints) {
                    val duration = dataPoint.getEndTime(TimeUnit.MILLISECONDS) - dataPoint.getStartTime(TimeUnit.MILLISECONDS)
                    totalMinutes += (duration / (1000 * 60)).toInt()
                }
            }
        }
        return@withContext totalMinutes
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FIT_PERMISSIONS && resultCode == Activity.RESULT_OK) {
            syncGoogleFitData()
        } else if (requestCode == REQUEST_FIT_PERMISSIONS) {
            Toast.makeText(requireContext(), "Разрешения не получены", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_FIT_PERMISSIONS = 1002
    }
}