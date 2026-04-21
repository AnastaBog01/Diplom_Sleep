package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ru.mauniver.kit.bivt.anasta.diplom_sleep.R
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord
import java.text.SimpleDateFormat
import java.util.*

class SleepRecordAdapter(
    private val records: List<SleepRecord>,
    private val onDeleteClick: (SleepRecord) -> Unit
) : RecyclerView.Adapter<SleepRecordAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sleep_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position], onDeleteClick)
    }

    override fun getItemCount() = records.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvTimeRange: TextView = itemView.findViewById(R.id.tvTimeRange)
        private val tvFactors: TextView = itemView.findViewById(R.id.tvFactors)
        private val tvQuality: TextView = itemView.findViewById(R.id.tvQuality)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(record: SleepRecord, onDeleteClick: (SleepRecord) -> Unit) {
            val startDate = Date(record.startTime)
            val endDate = Date(record.endTime)
            val sdfDate = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
            val durationHours = (record.endTime - record.startTime) / (1000f * 60 * 60)

            tvDate.text = sdfDate.format(startDate)
            tvDuration.text = String.format("%.1f ч", durationHours)
            tvTimeRange.text = "${sdfTime.format(startDate)} - ${sdfTime.format(endDate)}"
            tvFactors.text = record.factors ?: "—"
            tvQuality.text = if (record.quality != null) "⭐ ${record.quality}/10" else "—"

            btnDelete.setOnClickListener {
                onDeleteClick(record)
            }
        }
    }
}