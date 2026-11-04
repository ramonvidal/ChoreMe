package com.choreme.core.model.room

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** What kind of room it is: picks its sketch, its icon and the tasks suggested for it. */
enum class RoomType {
    LIVING_ROOM, BEDROOM, KIDS_ROOM, BATHROOM, KITCHEN, DINING_ROOM, OFFICE, LAUNDRY, GARAGE, GARDEN, BALCONY, HALLWAY, OTHER,
}

/** A small JPEG of the room. Compared by content, so a screen only redraws when the picture changed. */
class RoomPhoto(val jpeg: ByteArray) {
    override fun equals(other: Any?) = other is RoomPhoto && jpeg.contentEquals(other.jpeg)
    override fun hashCode() = jpeg.contentHashCode()
}

/** A room of the house the family looks after, e.g. "Bathroom 1". */
data class Room(
    val id: String,
    val name: String,
    val type: RoomType = RoomType.OTHER,
    val description: String = "",
    /** Their own picture; without one the app draws the standard sketch for the room's [type]. */
    val photo: RoomPhoto? = null,
) {
    companion object {
        const val MAX_NAME = 40
        const val MAX_DESCRIPTION = 500

        /** The most a stored picture may weigh (see firestore.rules); the app shrinks pictures well below it. */
        const val MAX_PHOTO_BYTES = 150_000
    }
}

/** A room with the chores that look after it: each task is a repeating chore tagged with the room. */
data class RoomOverview(val room: Room, val tasks: List<Chore>) {
    /** The newest time any of its tasks was done: "cleaned 5 days ago". Null when none ever was. */
    val lastDoneOn: LocalDate? get() = tasks.mapNotNull { it.lastDoneOn }.maxOrNull()

    /** The task that's due first, if any is waiting. */
    val next: Chore? get() = tasks.filter { it.isActive }.minWithOrNull(compareBy<Chore> { it.dueDate }.thenBy { it.title.lowercase() })

    fun lateCount(today: LocalDate): Int = tasks.count { it.isOverdue(today) }
}

/** Rooms by name, each with its tasks (the family's chores that carry its id). */
fun roomOverviews(rooms: List<Room>, chores: List<Chore>): List<RoomOverview> {
    val byRoom = chores.filter { it.roomId != null }.groupBy { it.roomId }
    return rooms
        .sortedBy { it.name.lowercase() }
        .map { room -> RoomOverview(room, byRoom[room.id].orEmpty().sortedBy { it.title.lowercase() }) }
}

/** How often a task repeats, in days, when it repeats every few days (the choice rooms offer). */
val Chore.everyDays: Int? get() = (recurrence as? Recurrence.EveryDays)?.days

/** Whole days from [date] to [today]; 0 is today. */
fun daysSince(date: LocalDate, today: LocalDate): Int = ChronoUnit.DAYS.between(date, today).toInt().coerceAtLeast(0)

/**
 * A new task for a room: a chore that repeats every [everyDays] days. When it was last done
 * ([lastDoneOn], if people know) decides when it's next due: "cleaned 4 days ago, every 10 days"
 * is due in 6. Unknown means due today.
 */
fun newRoomTask(
    roomId: String,
    title: String,
    template: ChoreTemplate,
    everyDays: Int,
    lastDoneOn: LocalDate?,
    today: LocalDate,
    points: Int = 1,
): Chore {
    val last = lastDoneOn?.takeIf { !it.isAfter(today) }
    return Chore(
        id = "",
        title = title.trim(),
        template = template,
        recurrence = Recurrence.EveryDays(everyDays),
        dueDate = last?.plusDays(everyDays.toLong()) ?: today,
        lastDoneOn = last,
        points = points,
        roomId = roomId,
    )
}

/** A task worth suggesting when adding one to a room: a ready-made chore, how often, and its picture. */
enum class TaskIdea(val template: ChoreTemplate, val everyDays: Int, val own: Boolean = false) {
    TIDY_UP(ChoreTemplate.TIDY_UP, 3),
    VACUUM(ChoreTemplate.VACUUM, 7),
    MAKE_BEDS(ChoreTemplate.MAKE_BEDS, 1),
    CLEAN_BATHROOM(ChoreTemplate.CLEAN_BATHROOM, 7),
    CLEAN_KITCHEN(ChoreTemplate.CLEAN_KITCHEN, 2),
    LAUNDRY(ChoreTemplate.LAUNDRY, 3),
    FOLD_CLOTHES(ChoreTemplate.FOLD_CLOTHES, 3),
    WATER_PLANTS(ChoreTemplate.WATER_PLANTS, 3),
    TRASH(ChoreTemplate.TRASH, 2),

    // Ideas with their own names (see the rooms module's texts).
    CHANGE_SHEETS(ChoreTemplate.MAKE_BEDS, 14, own = true),
    DUST(ChoreTemplate.TIDY_UP, 14, own = true),
    MOP(ChoreTemplate.VACUUM, 10, own = true),
    SWEEP(ChoreTemplate.VACUUM, 7, own = true),
    WINDOWS(ChoreTemplate.CLEAN_KITCHEN, 30, own = true),
    CLEAN_FRIDGE(ChoreTemplate.CLEAN_KITCHEN, 30, own = true),
    CLEAN_OVEN(ChoreTemplate.CLEAN_KITCHEN, 30, own = true),
    CLEAN_TOILET(ChoreTemplate.CLEAN_BATHROOM, 7, own = true),
    WASH_TOWELS(ChoreTemplate.LAUNDRY, 7, own = true),
    DECLUTTER(ChoreTemplate.TIDY_UP, 30, own = true),
    MOW_LAWN(ChoreTemplate.WATER_PLANTS, 14, own = true),
}

/** The tasks suggested for a kind of room, most useful first. */
fun ideasFor(type: RoomType): List<TaskIdea> = when (type) {
    RoomType.LIVING_ROOM -> listOf(TaskIdea.TIDY_UP, TaskIdea.VACUUM, TaskIdea.DUST, TaskIdea.WINDOWS)
    RoomType.BEDROOM -> listOf(TaskIdea.MAKE_BEDS, TaskIdea.CHANGE_SHEETS, TaskIdea.VACUUM, TaskIdea.DECLUTTER)
    RoomType.KIDS_ROOM -> listOf(TaskIdea.TIDY_UP, TaskIdea.CHANGE_SHEETS, TaskIdea.VACUUM, TaskIdea.DECLUTTER)
    RoomType.BATHROOM -> listOf(TaskIdea.CLEAN_BATHROOM, TaskIdea.CLEAN_TOILET, TaskIdea.MOP, TaskIdea.WASH_TOWELS)
    RoomType.KITCHEN -> listOf(TaskIdea.CLEAN_KITCHEN, TaskIdea.MOP, TaskIdea.CLEAN_FRIDGE, TaskIdea.CLEAN_OVEN, TaskIdea.TRASH)
    RoomType.DINING_ROOM -> listOf(TaskIdea.SWEEP, TaskIdea.DUST, TaskIdea.TIDY_UP)
    RoomType.OFFICE -> listOf(TaskIdea.TIDY_UP, TaskIdea.DUST, TaskIdea.VACUUM)
    RoomType.LAUNDRY -> listOf(TaskIdea.LAUNDRY, TaskIdea.FOLD_CLOTHES, TaskIdea.MOP)
    RoomType.GARAGE -> listOf(TaskIdea.SWEEP, TaskIdea.DECLUTTER)
    RoomType.GARDEN -> listOf(TaskIdea.WATER_PLANTS, TaskIdea.MOW_LAWN, TaskIdea.SWEEP)
    RoomType.BALCONY -> listOf(TaskIdea.SWEEP, TaskIdea.WATER_PLANTS)
    RoomType.HALLWAY -> listOf(TaskIdea.SWEEP, TaskIdea.MOP, TaskIdea.TIDY_UP)
    RoomType.OTHER -> listOf(TaskIdea.TIDY_UP, TaskIdea.VACUUM, TaskIdea.DUST)
}

/** How often tasks are offered to repeat, in days, as quick choices. */
val FREQUENCY_CHOICES: List<Int> = listOf(1, 3, 7, 10, 14, 30)

/** "Last done" quick choices, in days ago; null is "not sure" (due today). */
val LAST_DONE_CHOICES: List<Int?> = listOf(0, 1, 3, 7, 14, null)
