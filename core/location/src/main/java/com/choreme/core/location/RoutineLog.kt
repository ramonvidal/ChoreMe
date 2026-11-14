package com.choreme.core.location

import android.content.Context
import com.choreme.core.model.weather.Visit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This phone's own arrivals at the family's saved places, for noticing habits ("Gym on Tuesdays"). Kept on the phone
 * only, for about two months, and never sent anywhere. Also remembers the habits the person said "not now" to.
 */
@Singleton
class RoutineLog @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("routine", Context.MODE_PRIVATE)

    @Synchronized
    fun recordArrival(placeId: String, at: Instant) {
        val keep = at.minusSeconds(KEEP_DAYS * 24 * 3600).toEpochMilli()
        val lines = (prefs.getString(VISITS, "").orEmpty().lines().filter { it.isNotBlank() } + "${at.toEpochMilli()}|$placeId")
            .filter { it.substringBefore('|').toLongOrNull()?.let { ms -> ms >= keep } == true }
            .takeLast(MAX_VISITS)
        prefs.edit().putString(VISITS, lines.joinToString("\n")).putLong(LAST + placeId, at.toEpochMilli()).apply()
    }

    @Synchronized
    fun visits(zone: ZoneId = ZoneId.systemDefault()): List<Visit> =
        prefs.getString(VISITS, "").orEmpty().lines().mapNotNull { line ->
            val ms = line.substringBefore('|').toLongOrNull() ?: return@mapNotNull null
            val place = line.substringAfter('|', "").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            Visit(place, Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime())
        }

    /** The day this phone last arrived at each place, kept even after the visits themselves are forgotten. */
    fun lastVisits(zone: ZoneId = ZoneId.systemDefault()): Map<String, LocalDate> =
        prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(LAST) || value !is Long) null
            else key.removePrefix(LAST) to Instant.ofEpochMilli(value).atZone(zone).toLocalDate()
        }.toMap()

    /** Habits the person turned down, by their [com.choreme.core.model.weather.RoutineSuggestion.key]. */
    fun dismissed(): Set<String> = prefs.getStringSet(DISMISSED, emptySet()).orEmpty().toSet()

    @Synchronized
    fun dismiss(key: String) {
        prefs.edit().putStringSet(DISMISSED, dismissed() + key).apply()
    }

    /** Forgets everything (the person asked, or left the family). */
    @Synchronized
    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val VISITS = "visits"
        const val DISMISSED = "dismissed"
        const val LAST = "last_"
        const val KEEP_DAYS = 60L
        const val MAX_VISITS = 500
    }
}
