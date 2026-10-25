package com.choreme.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class AlertSettingsTest {
    private fun t(hour: Int, minute: Int = 0) = LocalTime.of(hour, minute)

    @Test
    fun `quiet hours across midnight`() {
        val night = QuietHours(t(22), t(7))
        assertTrue(night.contains(t(23)))
        assertTrue(night.contains(t(3)))
        assertTrue(night.contains(t(22)))
        assertFalse(night.contains(t(7)))
        assertFalse(night.contains(t(12)))
    }

    @Test
    fun `quiet hours within a day, and an empty window`() {
        val nap = QuietHours(t(14), t(16))
        assertTrue(nap.contains(t(15)))
        assertFalse(nap.contains(t(16, 30)))
        assertFalse(QuietHours(t(9), t(9)).contains(t(9)))
    }

    @Test
    fun `alerts are on until turned off`() {
        val settings = AlertSettings(disabled = setOf(AlertKind.ARRIVALS))
        assertTrue(settings.isEnabled(AlertKind.NEW_CHORES))
        assertFalse(settings.isEnabled(AlertKind.ARRIVALS))
        assertFalse(settings.isSilent(t(23)))
        assertTrue(settings.copy(quietHours = QuietHours.DEFAULT).isSilent(t(23)))
    }
}
