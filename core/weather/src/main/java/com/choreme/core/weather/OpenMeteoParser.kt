package com.choreme.core.weather

import com.choreme.core.model.weather.Forecast
import com.choreme.core.model.weather.WeatherHour
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime

/** Reads Open-Meteo's hourly answer ("hourly": {"time": [...], "temperature_2m": [...], ...}). */
internal object OpenMeteoParser {
    /** The hourly fields asked for, in the order the request lists them. */
    const val HOURLY_FIELDS = "temperature_2m,apparent_temperature,precipitation_probability,precipitation," +
        "wind_speed_10m,wind_gusts_10m,relative_humidity_2m,uv_index,weather_code,is_day"

    fun parse(json: String, fetchedAt: Instant): Forecast {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val hours = (0 until times.length()).mapNotNull { i ->
            val time = runCatching { LocalDateTime.parse(times.getString(i)) }.getOrNull() ?: return@mapNotNull null
            val temperature = hourly.number("temperature_2m", i) ?: return@mapNotNull null
            WeatherHour(
                time = time,
                temperatureC = temperature,
                feelsLikeC = hourly.number("apparent_temperature", i),
                rainProbability = hourly.number("precipitation_probability", i)?.toInt()?.coerceIn(0, 100) ?: 0,
                rainMm = hourly.number("precipitation", i) ?: 0.0,
                windKmh = hourly.number("wind_speed_10m", i) ?: 0.0,
                gustKmh = hourly.number("wind_gusts_10m", i) ?: 0.0,
                humidity = hourly.number("relative_humidity_2m", i)?.toInt()?.coerceIn(0, 100) ?: 50,
                uvIndex = hourly.number("uv_index", i) ?: 0.0,
                weatherCode = hourly.number("weather_code", i)?.toInt() ?: 0,
                isDay = (hourly.number("is_day", i) ?: 1.0) >= 1.0,
            )
        }
        return Forecast(hours, fetchedAt)
    }

    /** The number at [index] of the array called [name], or null when the field or the value is missing. */
    private fun JSONObject.number(name: String, index: Int): Double? {
        val array: JSONArray = optJSONArray(name) ?: return null
        if (index >= array.length() || array.isNull(index)) return null
        return array.optDouble(index).takeUnless { it.isNaN() }
    }
}
