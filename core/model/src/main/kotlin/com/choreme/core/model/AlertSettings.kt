package com.choreme.core.model

import java.time.LocalTime

/** The kinds of notification ChoreMe can post; each can be turned off per phone. */
enum class AlertKind {
    NEW_CHORES,
    APPROVALS,
    JOIN_REQUESTS,
    ARRIVALS,
    MORNING_SUMMARY,
}

/** A nightly window when notifications arrive silently, e.g. 22:00 to 07:00. */
data class QuietHours(val start: LocalTime, val end: LocalTime) {
    /** Handles windows that cross midnight. An empty window (start == end) is never quiet. */
    fun contains(time: LocalTime): Boolean = when {
        start == end -> false
        start < end -> time >= start && time < end
        else -> time >= start || time < end
    }

    companion object {
        val DEFAULT = QuietHours(LocalTime.of(22, 0), LocalTime.of(7, 0))
    }
}

/** This phone's notification choices. Everything is on until the person turns it off. */
data class AlertSettings(
    val disabled: Set<AlertKind> = emptySet(),
    /** Null when quiet hours are off. */
    val quietHours: QuietHours? = null,
) {
    fun isEnabled(kind: AlertKind) = kind !in disabled

    /** During quiet hours notifications still arrive, just without sound or vibration. */
    fun isSilent(time: LocalTime) = quietHours?.contains(time) == true
}
