package com.choreme.core.weather

import com.choreme.core.model.weather.AirForecast
import com.choreme.core.model.weather.AirHour
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDateTime

/** Reads Open-Meteo's air quality answer: the US index and the strongest of the pollen kinds, per hour. */
internal object AirParser {
    private val POLLEN = listOf("alder_pollen", "birch_pollen", "grass_pollen", "mugwort_pollen", "olive_pollen", "ragweed_pollen")
    const val HOURLY_FIELDS = "us_aqi,alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen"

    fun parse(json: String, fetchedAt: Instant): AirForecast {
        val hourly = JSONObject(json).getJSONObject("hourly")
        val times = hourly.getJSONArray("time")
        val hours = (0 until times.length()).mapNotNull { i ->
            val time = runCatching { LocalDateTime.parse(times.getString(i)) }.getOrNull() ?: return@mapNotNull null
            AirHour(
                time = time,
                usAqi = hourly.number("us_aqi", i)?.toInt(),
                pollen = POLLEN.mapNotNull { hourly.number(it, i) }.maxOrNull(),
            )
        }
        return AirForecast(hours, fetchedAt)
    }

    private fun JSONObject.number(name: String, index: Int): Double? {
        val array: JSONArray = optJSONArray(name) ?: return null
        if (index >= array.length() || array.isNull(index)) return null
        return array.optDouble(index).takeUnless { it.isNaN() }
    }
}
