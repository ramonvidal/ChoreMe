package com.choreme.core.model.room

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RoomHealthTest {
    private val today = LocalDate.of(2026, 11, 16)
    private val bathroom = Room("b", "Bathroom", RoomType.BATHROOM)
    private val kitchen = Room("k", "Kitchen", RoomType.KITCHEN)
    private val living = Room("l", "Living room", RoomType.LIVING_ROOM)

    private fun task(id: String, room: Room, due: LocalDate, status: ChoreStatus = ChoreStatus.OPEN, urgent: Boolean = false) =
        Chore(id = id, title = id, template = ChoreTemplate.TIDY_UP, dueDate = due, roomId = room.id, status = status, urgent = urgent)

    @Test
    fun `a room is late, due today or fine by its tasks`() {
        val overviews = roomOverviews(
            listOf(bathroom, kitchen, living),
            listOf(task("a", bathroom, today.minusDays(9)), task("b", kitchen, today), task("c", living, today.plusDays(3))),
        ).associateBy { it.room.id }
        assertEquals(RoomHealth.LATE, overviews.getValue("b").health(today))
        assertEquals(RoomHealth.DUE_TODAY, overviews.getValue("k").health(today))
        assertEquals(RoomHealth.OK, overviews.getValue("l").health(today))
    }

    @Test
    fun `late rooms are counted and listed first`() {
        val overviews = roomOverviews(
            listOf(bathroom, kitchen, living),
            listOf(task("a", living, today.plusDays(1)), task("b", kitchen, today), task("c", bathroom, today.minusDays(1))),
        )
        assertEquals(1, lateRoomCount(overviews, today))
        assertEquals(listOf("Bathroom", "Kitchen", "Living room"), overviews.byHealth(today).map { it.room.name })
    }

    @Test
    fun `the nudge is the longest overdue task nobody has taken`() {
        val overviews = roomOverviews(
            listOf(bathroom, kitchen),
            listOf(task("box", bathroom, today.minusDays(9)), task("floor", kitchen, today.minusDays(3)), task("oven", kitchen, today.minusDays(20), ChoreStatus.CLAIMED)),
        )
        val nudge = roomNudge(overviews, today)!!
        assertEquals("box", nudge.task.id)
        assertEquals(9, nudge.daysLate)
        assertEquals("Bathroom", nudge.room.name)
    }

    @Test
    fun `no nudge when nothing is late or the late ones have someone`() {
        assertNull(roomNudge(roomOverviews(listOf(bathroom), listOf(task("a", bathroom, today))), today))
        assertNull(roomNudge(roomOverviews(listOf(bathroom), listOf(task("a", bathroom, today.minusDays(2), ChoreStatus.CLAIMED))), today))
    }

    @Test
    fun `your tasks mix the house and the rooms, urgent first then by date, rooms tagged`() {
        val houseUrgent = Chore(id = "trash", title = "Trash", template = ChoreTemplate.TRASH, dueDate = today, urgent = true)
        val houseLater = Chore(id = "dishes", title = "Dishes", template = ChoreTemplate.DISHES, dueDate = today)
        val roomOld = task("mop", kitchen, today.minusDays(2))
        val list = yourTasks(listOf(houseLater, houseUrgent), listOf(roomOld), listOf(kitchen))
        assertEquals(listOf("trash", "mop", "dishes"), list.map { it.chore.id })
        assertEquals(listOf(null, "Kitchen", null), list.map { it.room?.name })
    }

    @Test
    fun `a room lists the tasks a person took, not the free ones or other people's`() {
        val mine = task("mirror", bathroom, today).copy(status = ChoreStatus.CLAIMED, claimedBy = "ana")
        val joined = task("floor", bathroom, today).copy(status = ChoreStatus.CLAIMED, claimedBy = "leo", helpers = setOf("ana"))
        val others = task("box", bathroom, today).copy(status = ChoreStatus.CLAIMED, claimedBy = "leo")
        val free = task("sink", bathroom, today)
        val overview = roomOverviews(listOf(bathroom), listOf(mine, joined, others, free)).single()
        assertEquals(setOf("mirror", "floor"), overview.takenBy("ana").map { it.id }.toSet())
        assertEquals(emptyList<Chore>(), overview.takenBy(null))
    }
}
