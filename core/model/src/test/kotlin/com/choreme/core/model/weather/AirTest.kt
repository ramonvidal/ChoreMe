package com.choreme.core.model.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime

class AirTest {
    private val noon = LocalDateTime.of(2026, 11, 16, 12, 0)

    @Test
    fun `air bands follow the US index`() {
        assertEquals(AirLevel.GOOD, airLevel(null))
        assertEquals(AirLevel.GOOD, airLevel(50))
        assertEquals(AirLevel.MODERATE, airLevel(51))
        assertEquals(AirLevel.POOR, airLevel(101))
        assertEquals(AirLevel.VERY_POOR, airLevel(151))
    }

    @Test
    fun `pollen reads as none where the service has none`() {
        assertEquals(PollenLevel.NONE, pollenLevel(null))
        assertEquals(PollenLevel.LOW, pollenLevel(5.0))
        assertEquals(PollenLevel.MODERATE, pollenLevel(30.0))
        assertEquals(PollenLevel.HIGH, pollenLevel(120.0))
    }

    @Test
    fun `the note takes the worst hour and stays quiet for ordinary days`() {
        val hours = listOf(AirHour(noon, 40, 2.0), AirHour(noon.plusHours(1), 120, 10.0))
        val note = airNote(hours)
        assertEquals(AirLevel.POOR, note.air)
        assertFalse(note.isQuiet)
        assertTrue(airNote(listOf(AirHour(noon, 80, 20.0))).isQuiet)
        assertTrue(airNote(emptyList()).isQuiet)
        assertFalse(airNote(listOf(AirHour(noon, 30, 90.0))).isQuiet)
    }

    @Test
    fun `between keeps the hours of the trip`() {
        val forecast = AirForecast(listOf(AirHour(noon, 10), AirHour(noon.plusHours(1), 20), AirHour(noon.plusHours(3), 30)), Instant.EPOCH)
        assertEquals(2, forecast.between(noon.plusMinutes(10), noon.plusHours(1).plusMinutes(30)).size)
    }
}
