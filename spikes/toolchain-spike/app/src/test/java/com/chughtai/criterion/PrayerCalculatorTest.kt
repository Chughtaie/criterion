package com.chughtai.criterion

import com.chughtai.criterion.prayer.PrayerCalculator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PrayerCalculatorTest {
    @Test fun karachiHanafi_2026_10_19() {
        val t = PrayerCalculator().karachi(2026, 10, 19)
        println("Karachi Hanafi 2026-10-19: $t")
        assertThat(t.fajr).startsWith("05:")
        assertThat(t.dhuhr).startsWith("12:")
        assertThat(t.asr).startsWith("16:")     // Hanafi (2x shadow) Asr, ~16:3x in late October
        assertThat(t.maghrib).startsWith("18:")
        assertThat(t.isha).startsWith("19:")
    }
}
