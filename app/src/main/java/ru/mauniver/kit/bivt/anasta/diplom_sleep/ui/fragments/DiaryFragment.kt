package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import ru.mauniver.kit.bivt.anasta.diplom_sleep.SleepTrackerApplication
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord
import ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.adapters.SleepRecordAdapter

class DiaryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: SleepRecordAdapter
    private var recordsList = mutableListOf<SleepRecord>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_diary, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvSleepRecords)
        tvEmpty = view.findViewById(R.id.tvEmptyDiary)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadRecords()
    }

    override fun onResume() {
        super.onResume()
        loadRecords()
    }

    private fun loadRecords() {
        lifecycleScope.launch {
            val app = requireContext().applicationContext as SleepTrackerApplication
            val records = app.repository.getAllRecords()
            recordsList.clear()
            recordsList.addAll(records)
            if (recordsList.isNotEmpty()) {
                tvEmpty.visibility = View.GONE
                adapter = SleepRecordAdapter(recordsList) { record ->
                    showDeleteConfirmationDialog(record)
                }
                recyclerView.adapter = adapter
            } else {
                tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun showDeleteConfirmationDialog(record: SleepRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить запись")
            .setMessage("Вы уверены, что хотите удалить эту запись о сне?")
            .setPositiveButton("Удалить") { _, _ ->
                deleteRecord(record)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteRecord(record: SleepRecord) {
        lifecycleScope.launch {
            val app = requireContext().applicationContext as SleepTrackerApplication
            app.repository.deleteRecord(record)
            loadRecords() // обновляем список
        }
    }
}