package com.choreme.core.model

import java.time.Duration
import java.time.Instant

/** Short-lived news someone shares with the family with one tap. */
enum class AnnouncementKind(
    /** After this it's old news and no longer shown. */
    val lasts: Duration,
) {
    /** "I'm at the shop": anything missing can still go on the list. Ends when shopping mode is turned off. */
    SHOPPING(Duration.ofHours(2)),

    /** "On my way home". */
    ON_MY_WAY(Duration.ofHours(1)),
}

/** At most one of each kind per person: sharing again just moves the time. */
data class Announcement(val memberId: String, val kind: AnnouncementKind, val at: Instant) {
    fun isCurrent(now: Instant): Boolean = !at.isAfter(now.plusSeconds(CLOCK_SLACK_SECONDS)) && at.plus(kind.lasts).isAfter(now)

    private companion object {
        /** Phones' clocks differ a little; a time just ahead of ours still counts. */
        const val CLOCK_SLACK_SECONDS = 300L
    }
}

/** Other people's current news of one kind, newest first. */
fun List<Announcement>.currentFromOthers(kind: AnnouncementKind, me: String, now: Instant): List<Announcement> =
    filter { it.kind == kind && it.memberId != me && it.isCurrent(now) }.sortedByDescending { it.at }

/** Tells one sharing apart from the next one by the same person (sharing again moves [Announcement.at]). */
val Announcement.key: String get() = "${memberId}_${kind.name}_${at.toEpochMilli()}"

/** What a phone should still notify about: current, from someone else, and not shown yet. */
fun List<Announcement>.toNotify(kind: AnnouncementKind, me: String, now: Instant, shown: Set<String>): List<Announcement> =
    currentFromOthers(kind, me, now).filter { it.key !in shown }
