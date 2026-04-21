package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import ru.mauniver.kit.bivt.anasta.diplom_sleep.receivers.NotificationReceiver
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.WelcomeActivity
import java.util.Calendar

class SettingsFragment : Fragment() {

    // UI элементы
    private lateinit var switchConnectFit: Switch
    private lateinit var switchAutoSync: Switch
    private lateinit var switchSleepReminder: Switch
    private lateinit var switchMorningSurvey: Switch
    private lateinit var btnSave: Button
    private lateinit var btnSleepReminderTime: Button
    private lateinit var btnMorningSurveyTime: Button
    private lateinit var btnSignOut: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        loadSavedSettings()
    }

    private fun initViews(view: View) {
        switchConnectFit = view.findViewById(R.id.switchConnectFit)
        switchAutoSync = view.findViewById(R.id.switchAutoSync)
        switchSleepReminder = view.findViewById(R.id.switchSleepReminder)
        switchMorningSurvey = view.findViewById(R.id.switchMorningSurvey)
        btnSave = view.findViewById(R.id.btnSave)
        btnSleepReminderTime = view.findViewById(R.id.btnSleepReminderTime)
        btnMorningSurveyTime = view.findViewById(R.id.btnMorningSurveyTime)
        btnSignOut = view.findViewById(R.id.btnSignOut)
    }

    private fun setupListeners() {
        // Обработка переключателей Google Fit (пока заглушка)
        switchConnectFit.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // TODO: подключение к Google Fit
            }
        }

        // Выбор времени для напоминания о сне
        btnSleepReminderTime.setOnClickListener {
            showTimePicker { selectedTime ->
                btnSleepReminderTime.text = selectedTime
                saveReminderTime("sleep_reminder_time", selectedTime)
                if (switchSleepReminder.isChecked) {
                    scheduleSleepReminderNotification(selectedTime)
                }
            }
        }

        // Выбор времени для утреннего опроса
        btnMorningSurveyTime.setOnClickListener {
            showTimePicker { selectedTime ->
                btnMorningSurveyTime.text = selectedTime
                saveReminderTime("morning_survey_time", selectedTime)
                if (switchMorningSurvey.isChecked) {
                    scheduleMorningSurveyNotification(selectedTime)
                }
            }
        }

        // Кнопка сохранения настроек
        btnSave.setOnClickListener {
            saveSettings()
            Toast.makeText(requireContext(), "Настройки сохранены", Toast.LENGTH_SHORT).show()
        }

        // Кнопка выхода из Google-аккаунта
        btnSignOut.setOnClickListener {
            signOutFromGoogle()
        }
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    // Работа с SharedPreferences
    private fun saveReminderTime(key: String, time: String) {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString(key, time).apply()
    }

    private fun getReminderTime(key: String, default: String): String {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getString(key, default) ?: default
    }

    private fun saveSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("switch_connect_fit", switchConnectFit.isChecked)
            .putBoolean("switch_auto_sync", switchAutoSync.isChecked)
            .putBoolean("switch_sleep_reminder", switchSleepReminder.isChecked)
            .putBoolean("switch_morning_survey", switchMorningSurvey.isChecked)
            .apply()

        // Дополнительно сохраняем время (хотя оно сохраняется при выборе)
        saveReminderTime("sleep_reminder_time", btnSleepReminderTime.text.toString())
        saveReminderTime("morning_survey_time", btnMorningSurveyTime.text.toString())

        // Перепланируем уведомления согласно новым настройкам
        val sleepTime = btnSleepReminderTime.text.toString()
        val surveyTime = btnMorningSurveyTime.text.toString()

        if (switchSleepReminder.isChecked) {
            scheduleSleepReminderNotification(sleepTime)
        } else {
            cancelNotification("sleep_reminder")
        }

        if (switchMorningSurvey.isChecked) {
            scheduleMorningSurveyNotification(surveyTime)
        } else {
            cancelNotification("morning_survey")
        }
    }

    private fun loadSavedSettings() {
        val prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Загружаем состояния переключателей
        switchConnectFit.isChecked = prefs.getBoolean("switch_connect_fit", false)
        switchAutoSync.isChecked = prefs.getBoolean("switch_auto_sync", false)
        switchSleepReminder.isChecked = prefs.getBoolean("switch_sleep_reminder", false)
        switchMorningSurvey.isChecked = prefs.getBoolean("switch_morning_survey", false)

        // Загружаем время
        val sleepTime = getReminderTime("sleep_reminder_time", "22:30")
        val surveyTime = getReminderTime("morning_survey_time", "08:00")
        btnSleepReminderTime.text = sleepTime
        btnMorningSurveyTime.text = surveyTime

        // Планируем уведомления, если соответствующие переключатели включены
        if (switchSleepReminder.isChecked) {
            scheduleSleepReminderNotification(sleepTime)
        } else {
            cancelNotification("sleep_reminder")
        }

        if (switchMorningSurvey.isChecked) {
            scheduleMorningSurveyNotification(surveyTime)
        } else {
            cancelNotification("morning_survey")
        }
    }

    // Уведомления

    private fun scheduleSleepReminderNotification(time: String) {
        scheduleDailyNotification("sleep_reminder", time, "Напоминание о сне", "Пора ложиться спать!")
    }

    private fun scheduleMorningSurveyNotification(time: String) {
        scheduleDailyNotification("morning_survey", time, "Утренний опрос", "Как вы спали? Оцените качество сна")
    }

    private fun scheduleDailyNotification(channelId: String, time: String, title: String, text: String) {
        // Проверяем разрешение на точные будильники
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Запрашиваем разрешение
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            putExtra("title", title)
            putExtra("text", text)
            putExtra("channelId", channelId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            channelId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            val parts = time.split(":")
            set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            set(Calendar.MINUTE, parts[1].toInt())
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)
    }

    private fun cancelNotification(channelId: String) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            channelId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun signOutFromGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        googleSignInClient.signOut().addOnCompleteListener {
            // Выход из Firebase
            FirebaseAuth.getInstance().signOut()

            // Сброс гостевого режима
            val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("guest_mode").apply()

            // Очистка настроек приложения
            val appPrefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            appPrefs.edit().clear().apply()

            // Переход на экран входа
            val intent = Intent(requireContext(), WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            activity?.finish()
        }
    }
}