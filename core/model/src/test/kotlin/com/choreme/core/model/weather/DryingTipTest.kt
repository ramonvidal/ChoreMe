package com.choreme.core.model.weather

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DryingTipTest {
    private val today = LocalDate.of(2026, 11, 16)
    private fun day(offset: Int, drying: DryingQuality) =
        DayOutlook(today.plusDays(offset.toLong()), Sky.CLEAR, 10.0, 20.0, 0, emptyList(), drying)

    @Test
    fun `laundry is due when a laundry chore is due and not done`() {
        val wash = Chore("1", "Laundry", ChoreTemplate.LAUNDRY, dueDate = today)
        assertTrue(laundryDue(listOf(wash), today))
        assertTrue(laundryDue(listOf(wash.copy(dueDate = today.minusDays(2))), today))
        assertFalse(laundryDue(listOf(wash.copy(dueDate = today.plusDays(1))), today))
        assertFalse(laundryDue(listOf(wash.copy(status = ChoreStatus.DONE)), today))
        assertFalse(laundryDue(listOf(Chore("2", "Dishes", ChoreTemplate.DISHES, dueDate = today)), today))
    }

    @Test
    fun `no tip without laundry`() {
        assertNull(dryingTip(false, listOf(day(0, DryingQuality.GOOD))))
        assertNull(dryingTip(true, emptyList()))
    }

    @Test
    fun `a good day today says hang it out`() {
        assertEquals(DryingTip.HangToday, dryingTip(true, listOf(day(0, DryingQuality.GOOD), day(1, DryingQuality.GOOD))))
    }

    @Test
    fun `a poor day waits for the next good one within three days`() {
        val days = listOf(day(0, DryingQuality.POOR), day(1, DryingQuality.POOR), day(2, DryingQuality.GOOD))
        assertEquals(DryingTip.WaitFor(today.plusDays(2)), dryingTip(true, days))
        // A good day four days away is too far to wait for.
        val far = listOf(0, 1, 2, 3).map { day(it, DryingQuality.POOR) } + day(4, DryingQuality.GOOD)
        assertEquals(DryingTip.Indoors, dryingTip(true, far))
    }

    @Test
    fun `an ordinary day with nothing better says nothing`() {
        assertNull(dryingTip(true, listOf(day(0, DryingQuality.OK), day(1, DryingQuality.OK))))
    }
}
