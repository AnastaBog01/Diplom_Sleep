package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.dialogs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import ru.mauniver.kit.bivt.anasta.diplom_sleep.SleepTrackerApplication
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord

class AddSleepRecordDialog : DialogFragment() {

    private lateinit var chipGroup: ChipGroup
    private lateinit var ratingBar: android.widget.RatingBar
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_sleep_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chipGroup = view.findViewById(R.id.chipGroupFactors)
        ratingBar = view.findViewById(R.id.ratingBarFeeling)
        saveButton = view.findViewById(R.id.btnSaveRecord)

        setupChips()
        saveButton.setOnClickListener { saveRecord() }
    }

    private fun setupChips() {
        val factors = listOf("Кофе", "Прогулка", "Вечеринка", "Книга", "Телефон", "Алкоголь", "Спорт", "Стресс")
        for (factor in factors) {
            val chip = Chip(requireContext())
            chip.text = factor
            chip.isCheckable = true
            chipGroup.addView(chip)
        }
    }

    private fun saveRecord() {
        val selectedFactors = (0 until chipGroup.childCount)
            .map { chipGroup.getChildAt(it) as Chip }
            .filter { it.isChecked }
            .map { it.text.toString() }
            .joinToString(",")

        val quality = ratingBar.rating.toInt()

        lifecycleScope.launch {
            val app = requireContext().applicationContext as SleepTrackerApplication
            // Находим самую свежую запись, у которой quality == null или factors == null
            val allRecords = app.repository.getAllRecords()
            val targetRecord = allRecords.find { it.quality == null || it.factors == null }
            if (targetRecord == null) {
                val now = System.currentTimeMillis()
                val newRecord = SleepRecord(
                    startTime = now - 8 * 60 * 60 * 1000L, // пример: сон 8 часов назад
                    endTime = now,
                    quality = quality,
                    factors = selectedFactors.ifEmpty { null }
                )
                app.repository.insertRecord(newRecord)
                Toast.makeText(requireContext(), "Создана новая запись", Toast.LENGTH_SHORT).show()
                dismiss()
                return@launch
            }
            // Обновляем запись
            val updatedRecord = targetRecord.copy(
                factors = selectedFactors.ifEmpty { null },
                quality = quality
            )
            app.repository.updateRecord(updatedRecord) // нужен метод update в репозитории
            Toast.makeText(requireContext(), "Запись обновлена", Toast.LENGTH_SHORT).show()
            dismiss()

        }

    }

    companion object {
        fun newInstance() = AddSleepRecordDialog()
    }
}