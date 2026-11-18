package com.choreme.core.location

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** The phone's own visits to saved places: kept for about two months, the last one per place kept longer. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class RoutineLogTest {
    private val log = RoutineLog(ApplicationProvider.getApplicationContext())
    private val utc = ZoneOffset.UTC
    private val now = Instant.parse("2026-11-18T08:00:00Z")

    @Test
    fun `arrivals are kept and read back in order`() {
        log.clear()
        log.recordArrival("gym", now.minusSeconds(86_400))
        log.recordArrival("work", now)
        assertEquals(listOf("gym", "work"), log.visits(utc).map { it.placeId })
        assertEquals(LocalDate.of(2026, 11, 18), log.lastVisits(utc)["work"])
    }

    @Test
    fun `visits older than two months are forgotten, but not the last day at each place`() {
        log.clear()
        log.recordArrival("school", now.minusSeconds(70L * 86_400))
        log.recordArrival("gym", now)
        assertEquals(listOf("gym"), log.visits(utc).map { it.placeId })
        assertEquals(LocalDate.of(2026, 9, 9), log.lastVisits(utc)["school"])
    }

    @Test
    fun `habits turned down stay turned down until cleared`() {
        log.clear()
        log.dismiss("gym|TUESDAY")
        assertTrue("gym|TUESDAY" in log.dismissed())
        log.clear()
        assertTrue(log.dismissed().isEmpty())
    }
}
