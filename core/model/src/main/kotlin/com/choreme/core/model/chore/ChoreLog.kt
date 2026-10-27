package com.choreme.core.model.chore

import com.choreme.core.model.Member
import java.time.LocalDate

/**
 * One time a chore was finished (approved, for a kid's). Recurring chores get one each time.
 * Done together, [helpers] did it with [doneBy] and each of them gets the points.
 */
data class ChoreDone(
    val choreId: String,
    val title: String,
    val doneBy: String,
    val on: LocalDate,
    val points: Int = 1,
    val helpers: Set<String> = emptySet(),
) {
    val people: Set<String> get() = setOf(doneBy) + helpers
}

/** The log entry for [after], if it was just finished (see [wasJustFinished]). */
fun choreDone(before: Chore, after: Chore): ChoreDone? {
    if (!after.wasJustFinished(before)) return null
    return ChoreDone(after.id, after.title, after.lastDoneBy ?: return null, after.lastDoneOn ?: return null, after.points, after.lastHelpers)
}

/** What one person did in the week. */
data class MemberWeek(val member: Member, val done: List<ChoreDone>) {
    val points: Int get() = done.sumOf { it.points }
}

/** The last 7 days up to [today]. */
fun weekStart(today: LocalDate): LocalDate = today.minusDays(6)

/**
 * The log, plus each chore's own last finish when the log is missing it: chores finished
 * before the log existed, or on a phone with an older version of the app.
 */
fun completeLog(log: List<ChoreDone>, chores: List<Chore>): List<ChoreDone> {
    val logged = log.map { it.choreId to it.on }.toSet()
    val missing = chores.mapNotNull { chore ->
        val by = chore.lastDoneBy ?: return@mapNotNull null
        val on = chore.lastDoneOn ?: return@mapNotNull null
        if (chore.id to on in logged) null else ChoreDone(chore.id, chore.title, by, on, chore.points, chore.lastHelpers)
    }
    return log + missing
}

private fun List<ChoreDone>.inWeek(today: LocalDate): List<ChoreDone> {
    val from = weekStart(today)
    return filter { !it.on.isBefore(from) && !it.on.isAfter(today) }
}

/** The family's points in the last 7 days: a chore done together counts once here. */
fun familyPoints(log: List<ChoreDone>, today: LocalDate): Int = log.inWeek(today).sumOf { it.points }

/**
 * Everyone in the family, most points first; people who did nothing are listed too. A chore
 * done together is in each of their weeks, with its full points.
 */
fun weeklySummary(log: List<ChoreDone>, members: List<Member>, today: LocalDate): List<MemberWeek> {
    val week = log.inWeek(today)
    return members
        .map { member -> MemberWeek(member, week.filter { member.id in it.people }.sortedByDescending { done -> done.on }) }
        .sortedWith(compareByDescending<MemberWeek> { it.points }.thenBy { it.member.name.lowercase() })
}

/** "This week: 23 points · Mia 9, Leo 7, Mom 5": the Sunday evening summary. Null when nobody did anything. */
data class WeekRecap(val totalPoints: Int, val chores: Int, val byPerson: List<Pair<String, Int>>)

fun weekRecap(log: List<ChoreDone>, members: List<Member>, today: LocalDate): WeekRecap? {
    val total = familyPoints(log, today)
    if (total == 0) return null
    val week = weeklySummary(log, members, today)
    return WeekRecap(total, log.inWeek(today).size, week.filter { it.points > 0 }.map { it.member.name to it.points })
}
