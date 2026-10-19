package com.chughtai.criterion.core.model

/** Pure-Kotlin domain model (no Android, no DI annotations). */
data class AyahRef(val surah: Int, val ayah: Int) {
    init {
        require(surah in 1..114) { "surah out of range: $surah" }
        require(ayah >= 1) { "ayah must be >= 1" }
    }
}

data class SearchHit(val ref: AyahRef, val text: String)

/** Normalises Arabic for search: strips tashkeel/tatweel/Quranic marks and folds letter variants. */
object ArabicNormalizer {
    private val marks = Regex("[ؐ-ًؚ-ٰٟۖ-ۭـ]")
    fun normalize(s: String): String = marks.replace(s, "")
        .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
        .replace('ى', 'ي').replace('ی', 'ي')
        .replace('ة', 'ه').replace('ک', 'ك')
}
