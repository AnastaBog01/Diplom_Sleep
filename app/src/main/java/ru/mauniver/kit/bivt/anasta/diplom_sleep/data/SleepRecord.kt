package ru.mauniver.kit.bivt.anasta.diplom_sleep.data
//Тут модель данных
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,  // время засыпания в миллисекундах
    val endTime: Long,  // время пробуждения в миллисекундах
    val quality: Int? = null,   // качество сна от 1 до 10 (из утреннего опроса)
    val notes: String? = null,   // заметки пользователя
    val factors: String? = null,   // храним через запятую
)