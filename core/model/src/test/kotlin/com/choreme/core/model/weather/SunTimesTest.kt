package com.choreme.core.model.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class SunTimesTest {
    @Test
    fun `Salvador in mid November: the sun sets before 18h`() {
        val sun = sunTimes(LocalDate.of(2026, 11, 15), -12.97, -38.50, -3.0)!!
        assertEquals(4.96f, sun.sunrise, 0.15f)
        assertEquals(17.67f, sun.sunset, 0.15f)
    }

    @Test
    fun `Berlin at midsummer has a very long day`() {
        val sun = sunTimes(LocalDate.of(2026, 6, 21), 52.52, 13.40, 2.0)!!
        assertEquals(4.71f, sun.sunrise, 0.15f)
        assertEquals(21.55f, sun.sunset, 0.15f)
    }

    @Test
    fun `the day is longer in the summer of each hemisphere`() {
        fun length(date: LocalDate, lat: Double) = sunTimes(date, lat, 0.0, 0.0)!!.let { it.sunset - it.sunrise }
        assertNotNull(length(LocalDate.of(2026, 12, 21), -23.5))
        assert(length(LocalDate.of(2026, 12, 21), -23.5) > length(LocalDate.of(2026, 6, 21), -23.5))
        assert(length(LocalDate.of(2026, 6, 21), 50.0) > length(LocalDate.of(2026, 12, 21), 50.0))
    }

    @Test
    fun `no sunrise inside the polar night`() {
        assertNull(sunTimes(LocalDate.of(2026, 12, 21), 80.0, 0.0, 0.0))
    }
}
