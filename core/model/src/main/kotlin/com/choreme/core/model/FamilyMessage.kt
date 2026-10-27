package com.choreme.core.model

import java.time.Duration
import java.time.Instant

/** A message to the whole family: "Dinner's ready!", "I'll be late, start without me". */
data class FamilyMessage(val id: String, val text: String, val from: String, val at: Instant) {
    companion object {
        const val MAX_LENGTH = 200

        /** Shown on the Family tab for this long; older ones are tidied away by whoever sent them. */
        val KEEP: Duration = Duration.ofDays(7)

        /** A phone that was off for longer doesn't pop up old news. */
        val NOTIFY_WITHIN: Duration = Duration.ofHours(12)
    }
}

/** The newest first, from the last [FamilyMessage.KEEP]. */
fun List<FamilyMessage>.recent(now: Instant): List<FamilyMessage> =
    filter { it.at.isAfter(now.minus(FamilyMessage.KEEP)) }.sortedByDescending { it.at }

/** What this phone should still pop up: from someone else, fresh, not shown yet. */
fun List<FamilyMessage>.toNotify(me: String, now: Instant, shown: Set<String>): List<FamilyMessage> =
    filter { it.from != me && it.id !in shown && it.at.isAfter(now.minus(FamilyMessage.NOTIFY_WITHIN)) }
        .sortedBy { it.at }
