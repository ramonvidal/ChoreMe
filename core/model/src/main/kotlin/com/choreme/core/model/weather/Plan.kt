package com.choreme.core.model.weather

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * One regular trip or outing in a grown-up's week: "Mon to Fri 8:00, to Work, by bike, 30 minutes".
 * [placeId] is a saved place of the family (the Map tab); the forecast is asked for it and for home.
 */
data class PlanEntry(
    val id: String,
    /** Whose it is (a member id). */
    val memberId: String,
    val title: String,
    val days: Set<DayOfWeek>,
    val start: LocalTime,
    val minutes: Int = DEFAULT_MINUTES,
    val mode: TripMode = TripMode.OTHER,
    val placeId: String? = null,
    /** True for the habits the app noticed and the person confirmed (see the routine learning). */
    val learned: Boolean = false,
) {
    fun isOn(day: DayOfWeek) = day in days

    companion object {
        const val DEFAULT_MINUTES = 30
        const val MIN_MINUTES = 5
        const val MAX_MINUTES = 12 * 60
        const val MAX_TITLE = 40
    }
}

/** One moment of a day with its weather: the entry, when it starts and what to know about it. */
data class Moment(val entry: PlanEntry, val start: LocalDateTime, val end: LocalDateTime, val advice: Advice)

/** The entries that happen on [date], in the order of the day. */
fun entriesOn(date: LocalDate, entries: List<PlanEntry>): List<PlanEntry> =
    entries.filter { it.isOn(date.dayOfWeek) }.sortedWith(compareBy({ it.start }, { it.title.lowercase() }))

/**
 * The day's moments with their weather. [hoursFor] gives the forecast hours for an entry's whole trip
 * (at home and at its place, merged); an entry with no forecast yet is left out, never guessed.
 */
fun momentsOn(
    date: LocalDate,
    entries: List<PlanEntry>,
    hoursFor: (entry: PlanEntry, from: LocalDateTime, to: LocalDateTime) -> List<WeatherHour>,
): List<Moment> = entriesOn(date, entries).mapNotNull { entry ->
    val start = date.atTime(entry.start)
    val end = start.plusMinutes(entry.minutes.toLong())
    val hours = hoursFor(entry, start, end)
    if (hours.isEmpty()) null else Moment(entry, start, end, adviceFor(hours, entry.mode))
}

/** What to tell the family in the morning: only the moments that need a word, plus whether the whole day is quiet. */
data class DaySummary(val moments: List<Moment>) {
    val worth: List<Moment> get() = moments.filter { !it.advice.isQuiet }
    val isQuiet: Boolean get() = worth.isEmpty()
}

fun summarize(moments: List<Moment>) = DaySummary(moments)
