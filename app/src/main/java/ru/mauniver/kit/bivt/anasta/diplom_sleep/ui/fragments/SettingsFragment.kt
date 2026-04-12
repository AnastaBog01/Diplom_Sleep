package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.example.sleeptracker.R

class SettingsFragment : Fragment() {

    private lateinit var switchConnectFit: Switch
    private lateinit var switchAutoSync: Switch
    private lateinit var switchSleepReminder: Switch
    private lateinit var switchMorningSurvey: Switch
    private lateinit var btnSave: Button

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
    }

    private fun setupListeners() {
        switchConnectFit.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Тут подключение к Google Fit
            }
        }

        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSavedSettings() {
        // Загрузка сохраненных настроек
    }

    private fun saveSettings() {
        val connectFit = switchConnectFit.isChecked
        val autoSync = switchAutoSync.isChecked
        val sleepReminder = switchSleepReminder.isChecked
        val morningSurvey = switchMorningSurvey.isChecked

        // Сохранение должно быть
    }
}