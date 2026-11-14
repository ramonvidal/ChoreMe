package com.choreme.core.model.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class OutlookTest {
    private val day = LocalDate.of(2026, 11, 16)
    private fun hour(h: Int, code: Int, temp: Double = 10.0, rain: Int = 0, on: LocalDate = day) =
        WeatherHour(on.atTime(h, 0), temp, rainProbability = rain, weatherCode = code)

    @Test
    fun `no hours means no outlook`() {
        assertNull(dayOutlook(day, emptyList(), emptyList()))
        assertNull(dayOutlook(day, listOf(hour(9, 0, on = day.plusDays(1))), emptyList()))
    }

    @Test
    fun `rain that lasts beats a clear majority`() {
        val hours = listOf(hour(8, 0), hour(9, 0), hour(10, 0), hour(11, 0), hour(14, 63, rain = 80), hour(15, 63, rain = 70))
        val outlook = dayOutlook(day, hours, emptyList())!!
        assertEquals(Sky.RAIN, outlook.sky)
        assertEquals(80, outlook.rainChance)
    }

    @Test
    fun `one wet hour does not make a rainy day`() {
        val hours = listOf(hour(8, 0), hour(9, 0), hour(10, 61, rain = 60))
        assertEquals(Sky.CLEAR, dayOutlook(day, hours, emptyList())!!.sky)
    }

    @Test
    fun `low and high count the whole day, even the night`() {
        val hours = listOf(hour(2, 0, temp = 3.0), hour(13, 0, temp = 12.5), hour(22, 0, temp = 6.0))
        val outlook = dayOutlook(day, hours, emptyList())!!
        assertEquals(3.0, outlook.lowC, 0.0)
        assertEquals(12.5, outlook.highC, 0.0)
    }
}
