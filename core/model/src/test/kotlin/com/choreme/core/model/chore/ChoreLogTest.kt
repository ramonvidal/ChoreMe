package com.choreme.core.model.chore

import com.choreme.core.model.Member
import com.choreme.core.model.Role
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChoreLogTest {
    private val today = LocalDate.of(2026, 10, 26)
    private val mom = Member("mom", "Mom", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID)
    private val mia = Member("mia", "Mia", Role.KID)

    @Test
    fun `finishing logs it, a kid's "done" only once approved`() {
        val dishes = Chore("1", "Dishes", recurrence = Recurrence.EveryDays(1), dueDate = today)
        assertEquals(ChoreDone("1", "Dishes", "mom", today), choreDone(dishes, dishes.complete(mom, today)))
        val pending = dishes.complete(leo, today)
        assertEquals(null, choreDone(dishes, pending))
        assertEquals(ChoreDone("1", "Dishes", "leo", today), choreDone(pending, pending.approve(mom, today)))
    }

    @Test
    fun `the week counts the last 7 days, most chores first, everyone listed`() {
        val log = listOf(
            ChoreDone("1", "Dishes", "leo", today),
            ChoreDone("1", "Dishes", "leo", today.minusDays(6)),
            ChoreDone("2", "Trash", "leo", today.minusDays(7)),
            ChoreDone("3", "Laundry", "mom", today.minusDays(1)),
        )
        val week = weeklySummary(log, listOf(mia, mom, leo), today)
        assertEquals(listOf("Leo" to 2, "Mom" to 1, "Mia" to 0), week.map { it.member.name to it.done.size })
    }

    @Test
    fun `bigger jobs count more, and the week is ranked by points`() {
        val log = listOf(
            ChoreDone("1", "Dishes", "leo", today, points = 1),
            ChoreDone("1", "Dishes", "leo", today, points = 1),
            ChoreDone("2", "Clean the garage", "mia", today, points = 5),
        )
        val week = weeklySummary(log, listOf(mia, leo), today)
        assertEquals(listOf("Mia" to 5, "Leo" to 2), week.map { it.member.name to it.points })
        assertEquals(WeekRecap(7, 3, listOf("Mia" to 5, "Leo" to 2)), weekRecap(week))
        assertEquals(null, weekRecap(weeklySummary(emptyList(), listOf(mia), today)))
    }

    @Test
    fun `finishing the same chore twice in a day counts twice`() {
        val dishes = Chore("1", "Dishes", recurrence = Recurrence.EveryDays(1), dueDate = today, points = 2)
        val first = dishes.complete(mom, today)
        assertEquals(ChoreDone("1", "Dishes", "mom", today, 2), choreDone(dishes, first))
        val second = first.complete(mom, today)
        assertEquals(ChoreDone("1", "Dishes", "mom", today, 2), choreDone(first, second))
        // Editing a chore isn't finishing it.
        assertEquals(null, choreDone(second, second.copy(dueDate = today.plusDays(3))))
    }

    @Test
    fun `chores finished before the log existed still count`() {
        val laundry = Chore("3", "Laundry", dueDate = today, status = ChoreStatus.DONE, lastDoneBy = "mia", lastDoneOn = today, points = 3)
        val logged = ChoreDone("1", "Dishes", "leo", today)
        val dishes = Chore("1", "Dishes", dueDate = today, lastDoneBy = "leo", lastDoneOn = today)
        assertEquals(listOf(logged, ChoreDone("3", "Laundry", "mia", today, 3)), completeLog(listOf(logged), listOf(dishes, laundry)))
    }

    @Test
    fun `the garden grows with the week and shows what's waiting`() {
        assertEquals(HomeScene(0, 0f, 0, lightsOn = true, tired = false), homeScene(0, overdue = 0, urgent = 0))
        val busy = homeScene(45, overdue = 4, urgent = 1)
        assertEquals(true, busy.fullBloom)
        assertEquals(HomeScene.MAX_FLOWERS, busy.flowers)
        assertEquals(false, busy.lightsOn)
        assertEquals(Weather.CLOUDY, busy.weather)
        assertEquals(true, busy.tired)
    }

    @Test
    fun `the garden counts overdue and urgent chores from the list`() {
        val late = Chore("1", "Dishes", dueDate = today.minusDays(1))
        val urgent = Chore("2", "Leak", dueDate = today, urgent = true)
        val doneUrgent = Chore("3", "Fuse", dueDate = today, urgent = true, status = ChoreStatus.DONE)
        val scene = homeScene(5, listOf(late, urgent, doneUrgent), today)
        assertEquals(false, scene.lightsOn)
        assertEquals(Weather.CLOUDY, scene.weather)
        assertEquals(1, scene.overgrown)
        assertEquals(false, scene.tired)
        assertEquals(2, scene.weeds) // the done one isn't waiting any more
    }

    @Test
    fun `the weather worsens as chores pile up`() {
        assertEquals(Weather.SUNNY, weatherFor(toDo = 2, overdue = 0, urgent = 0))
        assertEquals(Weather.CLOUDY, weatherFor(toDo = 1, overdue = 0, urgent = 1))
        assertEquals(Weather.CLOUDY, weatherFor(toDo = 3, overdue = 0, urgent = 0))
        assertEquals(Weather.RAIN, weatherFor(toDo = 4, overdue = 2, urgent = 0))
        assertEquals(Weather.STORM, weatherFor(toDo = 6, overdue = 3, urgent = 1))
        // Finishing chores clears it again.
        assertEquals(Weather.SUNNY, homeScene(10, listOf(Chore("1", "Dishes", dueDate = today, status = ChoreStatus.DONE)), today).weather)
    }

    @Test
    fun `adding a chore adds a weed and finishing it turns it into flowers`() {
        val dishes = Chore("1", "Dishes", dueDate = today, points = 2)
        val before = homeScene(3, listOf(dishes), today)
        val after = homeScene(3 + dishes.points, listOf(dishes.copy(status = ChoreStatus.DONE)), today)
        assertEquals(1, before.weeds)
        assertEquals(0, after.weeds)
        assertEquals(before.flowers + 2, after.flowers)
        val tomorrow = Chore("2", "Bins", dueDate = today.plusDays(1))
        assertEquals(0, homeScene(0, listOf(tomorrow), today).weeds) // not due yet
    }

    @Test
    fun `a saved chore remembers new points`() {
        val saved = listOf(SavedChore("garage", "Clean the garage", "mom", points = 3))
        assertEquals(saved[0].copy(points = 5), savedChoreToUpdate("clean the garage", 5, saved))
        assertEquals(null, savedChoreToUpdate("Clean the garage", 3, saved))
    }
}

class TimedReminderTest {
    @Test
    fun `a reminder time already passed today means tomorrow`() {
        val now = java.time.LocalDateTime.of(2026, 10, 26, 17, 30)
        assertEquals(now.withHour(18).withMinute(0), nextReminderTime(now, java.time.LocalTime.of(18, 0)))
        assertEquals(now.plusDays(1).withHour(9).withMinute(0), nextReminderTime(now, java.time.LocalTime.of(9, 0)))
    }
}
