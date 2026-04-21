package ru.mauniver.kit.bivt.anasta.diplom_sleep.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SleepRepository(private val dao: SleepDao) {
    suspend fun insertRecord(record: SleepRecord) {
        dao.insert(record)
    }

    fun getRecordsBetween(from: Long, to: Long): Flow<List<SleepRecord>> = flow {
        emit(dao.getRecordsBetween(from, to))
    }

    suspend fun getAllRecords(): List<SleepRecord> {
        return dao.getAllRecords()
    }

    suspend fun updateRecord(record: SleepRecord) {
        dao.update(record)
    }

    suspend fun deleteRecord(record: SleepRecord) {
        dao.delete(record)
    }
}