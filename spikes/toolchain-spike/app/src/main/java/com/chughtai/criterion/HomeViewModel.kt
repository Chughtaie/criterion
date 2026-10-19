package com.chughtai.criterion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chughtai.criterion.core.content.QuranContentDb
import com.chughtai.criterion.core.model.ArabicNormalizer
import com.chughtai.criterion.prayer.DayTimes
import com.chughtai.criterion.prayer.PrayerCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    prayer: PrayerCalculator,
    private val db: QuranContentDb,
) : ViewModel() {
    val times: DayTimes = prayer.karachi(2026, 10, 19)
    private val _hits = MutableStateFlow<List<String>>(emptyList())
    val hits: StateFlow<List<String>> = _hits

    fun search(q: String) = viewModelScope.launch {
        _hits.value = db.ayahDao().searchWords(ArabicNormalizer.normalize(q)).map { "${it.surah}:${it.ayah}" }
    }
}
