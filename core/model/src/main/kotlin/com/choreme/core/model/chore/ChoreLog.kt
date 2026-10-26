package com.choreme.core.model.chore

import com.choreme.core.model.Member
import java.time.LocalDate

/** One time a chore was finished (approved, for a kid's). Recurring chores get one each time. */
data class ChoreDone(val choreId: String, val title: String, val doneBy: String, val on: LocalDate)

/** The log entry for [after], if it was just finished (see [wasJustFinished]). */
fun choreDone(before: Chore, after: Chore): ChoreDone? {
    if (!after.wasJustFinished(before)) return null
    return ChoreDone(after.id, after.title, after.lastDoneBy ?: return null, after.lastDoneOn ?: return null)
}

/** What one person did in the week, for keeping things fair (no points). */
data class MemberWeek(val member: Member, val done: List<ChoreDone>)

/** The last 7 days up to [today]. */
fun weekStart(today: LocalDate): LocalDate = today.minusDays(6)

/** Everyone in the family, most chores first; people who did nothing are listed too. */
fun weeklySummary(log: List<ChoreDone>, members: List<Member>, today: LocalDate): List<MemberWeek> {
    val from = weekStart(today)
    val week = log.filter { !it.on.isBefore(from) && !it.on.isAfter(today) }.groupBy { it.doneBy }
    return members
        .map { MemberWeek(it, week[it.id].orEmpty().sortedByDescending { done -> done.on }) }
        .sortedWith(compareByDescending<MemberWeek> { it.done.size }.thenBy { it.member.name.lowercase() })
}
