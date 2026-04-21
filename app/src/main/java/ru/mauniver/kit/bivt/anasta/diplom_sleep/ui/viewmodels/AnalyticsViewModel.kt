package ru.mauniver.kit.bivt.anasta.diplom_sleep.ui.viewmodels
//Хранит данные для экрана аналитики
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRecord
import ru.mauniver.kit.bivt.anasta.diplom_sleep.data.SleepRepository
import kotlinx.coroutines.flow.firstOrNull

class AnalyticsViewModel(private val repository: SleepRepository) : ViewModel() {
    private val _records = MutableLiveData<List<SleepRecord>>(emptyList())
    val records: LiveData<List<SleepRecord>> = _records

    fun loadRecordsForLastDays(days: Int = 7) {
        viewModelScope.launch {
            val end = System.currentTimeMillis()
            val start = end - days * 24 * 60 * 60 * 1000L
            val flow = repository.getRecordsBetween(start, end) // это Flow<List<SleepRecord>>
            val list = flow.firstOrNull() ?: emptyList()       // превращаем Flow в List
            _records.postValue(list)
        }
    }

//    fun loadRecordsForLastDays(days: Int = 7) {
//        viewModelScope.launch {
//            val list = repository.getAllRecords() // предполагаем, что есть такой метод
//            _records.postValue(list)
//        }
//    }
}