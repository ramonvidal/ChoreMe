package com.choreme.core.model.weather

import com.choreme.core.model.Member
import java.time.LocalDate

/** Why a trip of the plan is left out of the weather for now. It comes back by itself when the reason ends. */
enum class PauseReason {
    /** Its owner is away (a trip, a week at grandma's): no weather for trips they are not making. */
    AWAY,

    /** A habit the app learned, not seen at its place for [UNSEEN_DAYS] days (the season changed, a new job). */
    UNSEEN,
}

/** Three weeks without a visit pauses a learned habit. */
const val UNSEEN_DAYS = 21L

/**
 * Why [entry] is paused today, or null when it is on. [member] is its owner. [lastVisit] is the day this phone last
 * arrived at the entry's place, which only the owner's own phone knows: a habit this phone has never seen is not
 * paused, since it cannot tell. Only learned habits fade; trips a parent typed in stay until a parent changes them.
 */
fun pauseReason(entry: PlanEntry, member: Member?, today: LocalDate, mine: Boolean, lastVisit: LocalDate?): PauseReason? = when {
    member != null && member.isAway(today) -> PauseReason.AWAY
    entry.learned && mine && lastVisit != null && lastVisit.isBefore(today.minusDays(UNSEEN_DAYS)) -> PauseReason.UNSEEN
    else -> null
}

/**
 * The reasons for every paused entry, by entry id. [lastVisits] maps a place to the day this phone last arrived there;
 * [myMemberId] is whose phone this is.
 */
fun pausedEntries(
    entries: List<PlanEntry>,
    members: List<Member>,
    today: LocalDate,
    myMemberId: String,
    lastVisits: Map<String, LocalDate>,
): Map<String, PauseReason> = entries.mapNotNull { entry ->
    val reason = pauseReason(
        entry, members.firstOrNull { it.id == entry.memberId }, today,
        mine = entry.memberId == myMemberId, lastVisit = entry.placeId?.let { lastVisits[it] },
    )
    reason?.let { entry.id to it }
}.toMap()

/** The entries that are on today: the weather is told for these only. */
fun activeEntries(
    entries: List<PlanEntry>,
    members: List<Member>,
    today: LocalDate,
    myMemberId: String,
    lastVisits: Map<String, LocalDate>,
): List<PlanEntry> {
    val paused = pausedEntries(entries, members, today, myMemberId, lastVisits)
    return entries.filter { it.id !in paused }
}
