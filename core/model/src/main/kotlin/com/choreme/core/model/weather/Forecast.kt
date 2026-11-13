package com.choreme.core.model.weather

import java.time.Instant
import java.time.LocalDateTime

/** One hour of forecast at one place. Times are the place's own clock, so a trip abroad reads right. */
data class WeatherHour(
    val time: LocalDateTime,
    val temperatureC: Double,
    /** What it feels like (wind and damp included); null when the service did not say. */
    val feelsLikeC: Double? = null,
    /** 0 to 100. */
    val rainProbability: Int = 0,
    val rainMm: Double = 0.0,
    val windKmh: Double = 0.0,
    val gustKmh: Double = 0.0,
    /** 0 to 100. */
    val humidity: Int = 50,
    val uvIndex: Double = 0.0,
    /** WMO weather code (0 clear ... 99 thunderstorm with hail). */
    val weatherCode: Int = 0,
    val isDay: Boolean = true,
) {
    val sky: Sky get() = skyOf(weatherCode)
}

/** What the weather service said about one place, hour by hour, and when we asked. */
data class Forecast(val hours: List<WeatherHour>, val fetchedAt: Instant) {
    /** The hours that overlap [from] to [to]: the one containing [from] up to the one containing [to]. */
    fun between(from: LocalDateTime, to: LocalDateTime): List<WeatherHour> {
        val start = from.withMinute(0).withSecond(0).withNano(0)
        val end = maxOf(to, from)
        return hours.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }
    }

    /** The hour [time] falls in, or null when the forecast does not cover it. */
    fun at(time: LocalDateTime): WeatherHour? = between(time, time).firstOrNull()
}

/** What the sky looks like: drives the little house's picture and the symbols. */
enum class Sky { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** WMO weather codes as the weather service sends them. */
fun skyOf(code: Int): Sky = when (code) {
    0 -> Sky.CLEAR
    1, 2 -> Sky.PARTLY_CLOUDY
    3 -> Sky.CLOUDY
    45, 48 -> Sky.FOG
    in 51..57 -> Sky.DRIZZLE
    in 61..67, in 80..82 -> Sky.RAIN
    in 71..77, 85, 86 -> Sky.SNOW
    in 95..99 -> Sky.STORM
    else -> Sky.CLOUDY
}
