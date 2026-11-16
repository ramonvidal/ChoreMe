package com.choreme.core.model.room

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How a room is doing: something overdue, something due today, or all in order. */
enum class RoomHealth { LATE, DUE_TODAY, OK }

fun RoomOverview.health(today: LocalDate): RoomHealth = when {
    lateCount(today) > 0 -> RoomHealth.LATE
    tasks.any { it.isActive && it.dueDate == today } -> RoomHealth.DUE_TODAY
    else -> RoomHealth.OK
}

/** How many rooms have something overdue: the red count on the Rooms sub-tab. */
fun lateRoomCount(rooms: List<RoomOverview>, today: LocalDate): Int = rooms.count { it.health(today) == RoomHealth.LATE }

/** Late rooms first, then those with something due today, then the rest; by name within each. */
fun List<RoomOverview>.byHealth(today: LocalDate): List<RoomOverview> =
    sortedWith(compareBy<RoomOverview> { it.health(today).ordinal }.thenBy { it.room.name.lowercase() })

/** "Bathroom: Clean the bathroom is [daysLate] days overdue and nobody has taken it", for the Home tab. */
data class RoomNudge(val room: Room, val task: Chore, val daysLate: Int)

/**
 * The room most worth a nudge: its overdue task that nobody is on (open), the longest overdue first. Tasks someone took
 * or finished are in hand, so they don't nag. Null when every late task already has someone.
 */
fun roomNudge(rooms: List<RoomOverview>, today: LocalDate): RoomNudge? =
    rooms.flatMap { overview ->
        overview.tasks.filter { it.status == ChoreStatus.OPEN && it.isOverdue(today) }.map { RoomNudge(overview.room, it, ChronoUnit.DAYS.between(it.dueDate, today).toInt()) }
    }.maxWithOrNull(compareBy<RoomNudge> { it.daysLate }.thenByDescending { it.room.name.lowercase() })

/**
 * Everything one person said they would do: the house's chores and the tasks of the rooms they took, urgent first, then
 * by due date. A task from a room carries its room (see [YourTask.room]) so lists can tag it.
 */
data class YourTask(val chore: Chore, val room: Room?)

fun yourTasks(houseMine: List<Chore>, roomMine: List<Chore>, rooms: List<Room>): List<YourTask> {
    val byId = rooms.associateBy { it.id }
    return (houseMine.map { YourTask(it, null) } + roomMine.map { YourTask(it, byId[it.roomId]) })
        .sortedWith(compareByDescending<YourTask> { it.chore.urgent }.thenBy { it.chore.dueDate }.thenBy { it.chore.title.lowercase() })
}
