package com.choreme.core.model.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class MovementWatchTest {
    private val start = Instant.parse("2026-11-04T10:00:00Z")
    private val home = GeoPoint(52.5200, 13.4050)
    // About 110 m north.
    private val up = GeoPoint(52.5210, 13.4050)

    @Test
    fun `just started counts as moving`() {
        assertFalse(MovementWatch(start).hasStopped(start.plusSeconds(60)))
    }

    @Test
    fun `sitting still for five minutes means stopped`() {
        val watch = MovementWatch(start)
        watch.onFix(home, start)
        watch.onFix(GeoPoint(52.52001, 13.40501), start.plus(Duration.ofMinutes(2)))
        assertFalse(watch.hasStopped(start.plus(Duration.ofMinutes(4))))
        assertTrue(watch.hasStopped(start.plus(Duration.ofMinutes(5))))
    }

    @Test
    fun `moving on keeps resetting the wait`() {
        val watch = MovementWatch(start)
        watch.onFix(home, start)
        watch.onFix(up, start.plus(Duration.ofMinutes(4)))
        assertFalse(watch.hasStopped(start.plus(Duration.ofMinutes(8))))
        assertTrue(watch.hasStopped(start.plus(Duration.ofMinutes(9))))
    }

    @Test
    fun `wobble of a few meters is not moving`() {
        val watch = MovementWatch(start)
        watch.onFix(home, start)
        watch.onFix(GeoPoint(52.52020, 13.40500), start.plus(Duration.ofMinutes(4)))
        assertTrue(watch.hasStopped(start.plus(Duration.ofMinutes(5))))
    }
}
