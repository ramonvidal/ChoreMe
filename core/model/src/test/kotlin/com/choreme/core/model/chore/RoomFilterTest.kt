package com.choreme.core.model.chore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RoomFilterTest {
    private val day = LocalDate.of(2026, 11, 5)
    private val plain = Chore("1", "Pay the rent", dueDate = day)
    private val inRoom = Chore("2", "Clean the toilet", dueDate = day, roomId = "bath1")
    private val all = listOf(plain, inRoom)

    @Test
    fun `all keeps everything`() = assertEquals(all, all.filteredBy(RoomFilter.ALL))

    @Test
    fun `rooms keeps only tasks of a room`() = assertEquals(listOf(inRoom), all.filteredBy(RoomFilter.ROOMS))

    @Test
    fun `no room keeps only the others`() = assertEquals(listOf(plain), all.filteredBy(RoomFilter.NO_ROOM))

    @Test
    fun `the filter only matters once some chore belongs to a room`() {
        assertTrue(all.hasRoomTasks)
        assertFalse(listOf(plain).hasRoomTasks)
    }
}
