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

    fun getAllRecords(): Flow<List<SleepRecord>> = flow {
        emit(dao.getAllRecords())
    }
}