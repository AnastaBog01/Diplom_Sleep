package ru.mauniver.kit.bivt.anasta.diplom_sleep.data
//Тут запросы к бд
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SleepDao {
    @Insert
    suspend fun insert(record: SleepRecord)

    @Query("SELECT * FROM sleep_records WHERE endTime >= :from AND endTime <= :to ORDER BY startTime DESC")
    suspend fun getRecordsBetween(from: Long, to: Long): List<SleepRecord>

    @Query("SELECT * FROM sleep_records ORDER BY startTime DESC")
    suspend fun getAllRecords(): List<SleepRecord>

    @Update
    suspend fun update(record: SleepRecord)

    @Delete
    suspend fun delete(record: SleepRecord)
}