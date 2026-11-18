package com.choreme.core.data.remote

import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.DUE_DATE
import com.choreme.core.data.remote.FirestoreSchema.NOTIFY_WHEN_DONE
import com.choreme.core.data.remote.FirestoreSchema.POINTS
import com.choreme.core.data.remote.FirestoreSchema.STATUS
import com.choreme.core.data.remote.FirestoreSchema.TEMPLATE
import com.choreme.core.data.remote.FirestoreSchema.TITLE
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** A chore written to Firestore and read back is the same chore, whatever kind of repeat it has. */
class ChoreMappingTest {
    private val day = LocalDate.of(2026, 11, 18)

    private fun roundTrip(chore: Chore, reportedBy: String? = null): Chore? =
        choreFrom(chore.id, chore.toFields() + (CREATED_BY to reportedBy))

    @Test
    fun `every kind of chore comes back the same`() {
        val chores = listOf(
            Chore(id = "a", title = "Dishes", template = ChoreTemplate.DISHES, dueDate = day),
            Chore(id = "b", title = "Bins", template = ChoreTemplate.TRASH, dueDate = day, recurrence = Recurrence.EveryDays(2), urgent = true, points = 3),
            Chore(
                id = "c", title = "Plants", template = ChoreTemplate.WATER_PLANTS, dueDate = day,
                recurrence = Recurrence.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY)), repeatUntil = day.plusMonths(2),
                status = ChoreStatus.CLAIMED, claimedBy = "leo", helpers = setOf("mia"), assignedTo = setOf("leo"),
            ),
            Chore(
                id = "d", title = "Rent", template = ChoreTemplate.OTHER, dueDate = day, recurrence = Recurrence.Monthly(5),
                status = ChoreStatus.PENDING_APPROVAL, claimedBy = "mia", completedBy = "mia", lastDoneBy = "leo",
                lastDoneOn = day.minusDays(30), lastHelpers = setOf("mia"), notifyWhenDone = false, doneCount = 4, roomId = "kitchen",
            ),
        )
        chores.forEach { assertEquals(it, roundTrip(it)) }
    }

    @Test
    fun `who reported it comes from the creator field`() {
        val chore = Chore(id = "a", title = "Leak", template = ChoreTemplate.OTHER, dueDate = day)
        assertEquals("leo", roundTrip(chore, reportedBy = "leo")?.reportedBy)
    }

    @Test
    fun `old documents get the defaults of their time`() {
        val old = mapOf(TITLE to "Dishes", TEMPLATE to "NOT_A_TEMPLATE", DUE_DATE to "2026-11-18", STATUS to "OPEN", POINTS to 99L)
        val chore = choreFrom("x", old)!!
        assertEquals(ChoreTemplate.OTHER, chore.template)
        assertTrue(chore.notifyWhenDone)
        assertEquals(Chore.POINTS.last, chore.points)
        assertEquals(Recurrence.Once, chore.recurrence)
    }

    @Test
    fun `a broken document is skipped, not a crash`() {
        assertNull(choreFrom("x", mapOf(TITLE to "No date", STATUS to "OPEN")))
        assertNull(choreFrom("x", mapOf(TITLE to "Bad status", DUE_DATE to "2026-11-18", STATUS to "MAYBE")))
        assertNull(choreFrom("x", mapOf(TITLE to "Bad date", DUE_DATE to "tomorrow", STATUS to "OPEN", NOTIFY_WHEN_DONE to false)))
    }
}
