package com.choreme.core.model.chore

/** Which chores to show: all of them, only those that belong to a room, or only those that don't. */
enum class RoomFilter { ALL, ROOMS, NO_ROOM }

fun Chore.matches(filter: RoomFilter): Boolean = when (filter) {
    RoomFilter.ALL -> true
    RoomFilter.ROOMS -> roomId != null
    RoomFilter.NO_ROOM -> roomId == null
}

fun List<Chore>.filteredBy(by: RoomFilter): List<Chore> =
    if (by == RoomFilter.ALL) this else this.filter { it.matches(by) }

/** Whether the filter would change anything: only when some chores belong to a room. */
val List<Chore>.hasRoomTasks: Boolean get() = any { it.roomId != null }
