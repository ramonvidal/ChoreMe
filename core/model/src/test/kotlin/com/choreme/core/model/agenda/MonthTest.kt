package com.choreme.core.model.agenda

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class MonthTest {
    @Test
    fun `grid starts on the Monday before the 1st and covers six weeks`() {
        // October 2026 starts on a Thursday.
        val grid = monthGrid(YearMonth.of(2026, 10))
        assertEquals(6, grid.size)
        assertEquals(LocalDate.of(2026, 9, 28), grid.first().first())
        assertEquals(DayOfWeek.MONDAY, grid.first().first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 8), grid.last().last())
        assertEquals(LocalDate.of(2026, 9, 28) to LocalDate.of(2026, 11, 9), monthGridRange(YearMonth.of(2026, 10)))
    }

    @Test
    fun `a month starting on Monday starts on its 1st`() {
        // June 2026 starts on a Monday.
        assertEquals(LocalDate.of(2026, 6, 1), monthGrid(YearMonth.of(2026, 6)).first().first())
    }
}
