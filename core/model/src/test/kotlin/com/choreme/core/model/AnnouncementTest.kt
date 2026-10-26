package com.choreme.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

class AnnouncementTest {
    private val now = Instant.parse("2026-10-26T18:00:00Z")

    @Test
    fun `news is shown while fresh, and never your own`() {
        val list = listOf(
            Announcement("mom", AnnouncementKind.SHOPPING, now.minus(Duration.ofMinutes(30))),
            Announcement("dad", AnnouncementKind.SHOPPING, now.minus(Duration.ofHours(3))),
            Announcement("leo", AnnouncementKind.ON_MY_WAY, now.minus(Duration.ofMinutes(10))),
            Announcement("me", AnnouncementKind.SHOPPING, now),
        )
        assertEquals(listOf("mom"), list.currentFromOthers(AnnouncementKind.SHOPPING, "me", now).map { it.memberId })
        assertEquals(listOf("leo"), list.currentFromOthers(AnnouncementKind.ON_MY_WAY, "me", now).map { it.memberId })
    }

    @Test
    fun `each sharing is notified once, and sharing again counts as new`() {
        val first = Announcement("mom", AnnouncementKind.SHOPPING, now.minus(Duration.ofMinutes(5)))
        assertEquals(listOf(first), listOf(first).toNotify(AnnouncementKind.SHOPPING, "me", now, shown = emptySet()))
        assertEquals(emptyList<Announcement>(), listOf(first).toNotify(AnnouncementKind.SHOPPING, "me", now, shown = setOf(first.key)))
        val again = first.copy(at = now)
        assertEquals(listOf(again), listOf(again).toNotify(AnnouncementKind.SHOPPING, "me", now, shown = setOf(first.key)))
    }

    @Test
    fun `on my way is old news after an hour`() {
        val leo = Announcement("leo", AnnouncementKind.ON_MY_WAY, now)
        assertEquals(true, leo.isCurrent(now.plus(Duration.ofMinutes(59))))
        assertEquals(false, leo.isCurrent(now.plus(Duration.ofMinutes(61))))
    }
}

class AwayTest {
    private val today = LocalDate.of(2026, 10, 26)

    @Test
    fun `away up to and including the last day`() {
        val leo = Member("leo", "Leo", Role.KID, awayUntil = today.plusDays(2))
        assertEquals(true, leo.isAway(today))
        assertEquals(true, leo.isAway(today.plusDays(2)))
        assertEquals(false, leo.isAway(today.plusDays(3)))
        assertEquals(false, leo.copy(awayUntil = null).isAway(today))
    }
}

class NoteTest {
    @Test
    fun `the writer and parents can edit a note, and the newest comes first`() {
        val note = Note("1", "Wi-Fi: hunter2", createdBy = "leo", updatedAt = Instant.parse("2026-10-20T10:00:00Z"))
        assertEquals(true, note.canEdit(Member("leo", "Leo", Role.KID)))
        assertEquals(false, note.canEdit(Member("mia", "Mia", Role.KID)))
        assertEquals(true, note.canEdit(Member("mom", "Mom", Role.PARENT)))
        val newer = note.copy(id = "2", updatedAt = Instant.parse("2026-10-25T10:00:00Z"))
        assertEquals(listOf("2", "1"), listOf(note, newer).board().map { it.id })
    }
}

class GreetingTest {
    @Test
    fun `the greeting follows the time of day`() {
        assertEquals(DayPart.MORNING, dayPart(java.time.LocalTime.of(7, 30)))
        assertEquals(DayPart.AFTERNOON, dayPart(java.time.LocalTime.of(12, 0)))
        assertEquals(DayPart.EVENING, dayPart(java.time.LocalTime.of(18, 0)))
        assertEquals(DayPart.EVENING, dayPart(java.time.LocalTime.of(2, 0)))
    }
}
