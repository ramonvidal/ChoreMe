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

class BuildMonthTest {
    private val zone = java.time.ZoneId.of("Europe/Madrid")
    private val today = LocalDate.of(2026, 10, 25)

    @Test
    fun `late chores sit on today, future ones on their day`() {
        val late = com.choreme.core.model.chore.Chore(id = "1", title = "Trash", dueDate = today.minusDays(3))
        val soon = com.choreme.core.model.chore.Chore(id = "2", title = "Vacuum", dueDate = today.plusDays(4))
        val month = buildMonth(emptyList(), listOf(late, soon), YearMonth.of(2026, 10), today, zone)
        assertEquals(listOf("Trash"), month[today]?.chores?.map { it.title })
        assertEquals(listOf("Vacuum"), month[today.plusDays(4)]?.chores?.map { it.title })
        assertEquals(null, month[today.minusDays(3)]?.chores?.takeIf { it.isNotEmpty() })
    }
}
