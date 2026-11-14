package com.choreme.core.model.weather

import java.time.Instant
import java.time.LocalDateTime

/** One hour of air at one place: the US air quality index and the strongest pollen in the air (grains per m3). */
data class AirHour(val time: LocalDateTime, val usAqi: Int? = null, val pollen: Double? = null)

/** The air quality service's answer, hour by hour, and when we asked. */
data class AirForecast(val hours: List<AirHour>, val fetchedAt: Instant) {
    fun between(from: LocalDateTime, to: LocalDateTime): List<AirHour> {
        val start = from.withMinute(0).withSecond(0).withNano(0)
        val end = maxOf(to, from)
        return hours.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }
    }
}

enum class AirLevel { GOOD, MODERATE, POOR, VERY_POOR }

enum class PollenLevel { NONE, LOW, MODERATE, HIGH }

/** US AQI bands: 0-50 good, 51-100 moderate, 101-150 poor (for sensitive people), above that very poor. */
fun airLevel(usAqi: Int?): AirLevel = when {
    usAqi == null || usAqi <= 50 -> AirLevel.GOOD
    usAqi <= 100 -> AirLevel.MODERATE
    usAqi <= 150 -> AirLevel.POOR
    else -> AirLevel.VERY_POOR
}

/** Grains per m3 of the strongest kind of pollen; the service has none outside Europe, which reads as none. */
fun pollenLevel(grains: Double?): PollenLevel = when {
    grains == null || grains < 1.0 -> PollenLevel.NONE
    grains < 30.0 -> PollenLevel.LOW
    grains < 80.0 -> PollenLevel.MODERATE
    else -> PollenLevel.HIGH
}

/** What to say about the air: the worst of the hours looked at. */
data class AirNote(val air: AirLevel = AirLevel.GOOD, val pollen: PollenLevel = PollenLevel.NONE) {
    /** Worth a line only when sensitive people would feel it: poor air or at least moderate pollen. */
    val isQuiet: Boolean get() = air < AirLevel.POOR && pollen < PollenLevel.MODERATE
}

fun airNote(hours: List<AirHour>): AirNote = AirNote(
    air = airLevel(hours.mapNotNull { it.usAqi }.maxOrNull()),
    pollen = pollenLevel(hours.mapNotNull { it.pollen }.maxOrNull()),
)
