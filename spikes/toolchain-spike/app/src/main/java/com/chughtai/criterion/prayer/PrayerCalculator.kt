package com.chughtai.criterion.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.Madhab
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

data class DayTimes(val fajr: String, val sunrise: String, val dhuhr: String, val asr: String, val maghrib: String, val isha: String)

class PrayerCalculator @Inject constructor() {
    fun karachi(year: Int, month: Int, day: Int): DayTimes {
        val params = CalculationMethod.KARACHI.parameters.copy(madhab = Madhab.HANAFI)
        val pt = PrayerTimes(Coordinates(24.8607, 67.0011), DateComponents(year, month, day), params)
        val tz = TimeZone.of("Asia/Karachi")
        fun f(i: kotlin.time.Instant) = i.toLocalDateTime(tz).time.toString()
        return DayTimes(f(pt.fajr), f(pt.sunrise), f(pt.dhuhr), f(pt.asr), f(pt.maghrib), f(pt.isha))
    }
}
