package com.choreme.core.model.weather

import java.time.LocalDate

/** One day of the week ahead: how the sky looks, how warm it gets, and the family's trips with their weather. */
data class DayOutlook(
    val date: LocalDate,
    val sky: Sky,
    val lowC: Double,
    val highC: Double,
    /** The highest chance of rain in the daytime, 0 to 100. */
    val rainChance: Int,
    val moments: List<Moment>,
)

/** The daytime hours the picture of a day is taken from. */
private const val FIRST_HOUR = 7
private const val LAST_HOUR = 20

/**
 * The outlook of [date] from the [hours] of that day (any hours; only the daytime ones pick the sky), or null when
 * the forecast does not cover it. The sky is the worst kind of weather that lasts at least two daytime hours
 * (rain beats cloud), else the most common one.
 */
fun dayOutlook(date: LocalDate, hours: List<WeatherHour>, moments: List<Moment>): DayOutlook? {
    val today = hours.filter { it.time.toLocalDate() == date }
    if (today.isEmpty()) return null
    val day = today.filter { it.time.hour in FIRST_HOUR..LAST_HOUR }.ifEmpty { today }
    val counts = day.groupingBy { it.sky }.eachCount()
    val sky = counts.filter { (sky, count) -> sky >= Sky.DRIZZLE && count >= 2 }.keys.maxOrNull()
        ?: counts.maxByOrNull { it.value }!!.key
    return DayOutlook(
        date = date,
        sky = sky,
        lowC = today.minOf { it.temperatureC },
        highC = today.maxOf { it.temperatureC },
        rainChance = day.maxOf { it.rainProbability },
        moments = moments,
    )
}
