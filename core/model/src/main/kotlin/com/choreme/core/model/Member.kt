package com.choreme.core.model

import java.time.LocalDate

enum class Role { PARENT, KID }

data class Member(
    val id: String,
    val name: String,
    val role: Role,
    /** Index into the member color palette. */
    val color: Int = 0,
    /** Signed-in devices linked to this member. Kids without a device have none. */
    val uids: List<String> = emptyList(),
    /** Away (a trip, a week at grandma's) up to and including this day; null when home. */
    val awayUntil: LocalDate? = null,
) {
    /** Away people get no chore reminders, and aren't offered as "done by". */
    fun isAway(today: LocalDate): Boolean = awayUntil != null && !today.isAfter(awayUntil)
}
