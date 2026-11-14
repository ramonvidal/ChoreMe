package com.choreme.core.weather

import android.content.Context
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.weather.AirForecast
import com.choreme.core.model.weather.AirHour
import com.choreme.core.model.weather.Forecast
import com.choreme.core.model.weather.WeatherHour
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToLong

/**
 * Forecasts from Open-Meteo (free for non-commercial use, no key or account; the data is CC BY 4.0, credited in
 * the app). Only the place's coordinates, rounded to about 1 km, ever leave the phone: no id, no family, no name.
 * Answers are kept for a few hours, and an older one still beats nothing when the phone is offline.
 */
@Singleton
class WeatherRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("weather", Context.MODE_PRIVATE)
    private val memory = ConcurrentHashMap<String, Forecast>()
    private val airMemory = ConcurrentHashMap<String, AirForecast>()

    /** The forecast for [point], or null when there is none and none could be fetched. */
    suspend fun forecast(point: GeoPoint, now: Instant = Instant.now()): Forecast? = withContext(Dispatchers.IO) {
        val key = key(point)
        val cached = memory[key] ?: readDisk(key)?.also { memory[key] = it }
        if (cached != null && Duration.between(cached.fetchedAt, now) < FRESH_FOR) return@withContext cached
        val fetched = runCatching { download(point) }.getOrNull()
        if (fetched != null) {
            val fresh = runCatching { OpenMeteoParser.parse(fetched, now) }.getOrNull()
            if (fresh != null) {
                memory[key] = fresh
                prefs.edit().putString(key, fetched).putLong("$key@", now.toEpochMilli()).apply()
                return@withContext fresh
            }
        }
        cached?.takeIf { Duration.between(it.fetchedAt, now) < STALE_LIMIT }
    }

    /**
     * The hours of a whole trip from [from] to [to], at every place it touches ([points]: home and the
     * destination): all their hours together, so the worst of the way counts.
     */
    suspend fun hoursFor(points: List<GeoPoint>, from: LocalDateTime, to: LocalDateTime): List<WeatherHour> =
        points.distinctBy { key(it) }.flatMap { point -> forecast(point)?.between(from, to).orEmpty() }

    /** The air quality and pollen for [point] (same rounded coordinates, same cache rules), or null when unknown. */
    suspend fun air(point: GeoPoint, now: Instant = Instant.now()): AirForecast? = withContext(Dispatchers.IO) {
        val key = "air:" + key(point)
        val cached = airMemory[key] ?: readAirDisk(key)?.also { airMemory[key] = it }
        if (cached != null && Duration.between(cached.fetchedAt, now) < FRESH_FOR) return@withContext cached
        val coarse = coarse(point)
        val url = String.format(
            Locale.ROOT, "%s?latitude=%.2f&longitude=%.2f&hourly=%s&timezone=auto&forecast_days=%d",
            AIR_ENDPOINT, coarse.latitude, coarse.longitude, AirParser.HOURLY_FIELDS, AIR_DAYS,
        )
        val fresh = runCatching { get(url) }.getOrNull()?.let { raw ->
            runCatching { AirParser.parse(raw, now) }.getOrNull()?.also {
                airMemory[key] = it
                prefs.edit().putString(key, raw).putLong("$key@", now.toEpochMilli()).apply()
            }
        }
        fresh ?: cached?.takeIf { Duration.between(it.fetchedAt, now) < STALE_LIMIT }
    }

    /** The air of a whole trip, at every place it touches, like [hoursFor]. */
    suspend fun airFor(points: List<GeoPoint>, from: LocalDateTime, to: LocalDateTime): List<AirHour> =
        points.distinctBy { key(it) }.flatMap { point -> air(point)?.between(from, to).orEmpty() }

    private fun readAirDisk(key: String): AirForecast? {
        val raw = prefs.getString(key, null) ?: return null
        val at = prefs.getLong("$key@", 0L).takeIf { it > 0 } ?: return null
        return runCatching { AirParser.parse(raw, Instant.ofEpochMilli(at)) }.getOrNull()
    }

    private fun readDisk(key: String): Forecast? {
        val raw = prefs.getString(key, null) ?: return null
        val at = prefs.getLong("$key@", 0L).takeIf { it > 0 } ?: return null
        return runCatching { OpenMeteoParser.parse(raw, Instant.ofEpochMilli(at)) }.getOrNull()
    }

    private fun download(point: GeoPoint): String {
        val coarse = coarse(point)
        return get(
            String.format(
                Locale.ROOT,
                "%s?latitude=%.2f&longitude=%.2f&hourly=%s&timezone=auto&forecast_days=%d&wind_speed_unit=kmh",
                ENDPOINT, coarse.latitude, coarse.longitude, OpenMeteoParser.HOURLY_FIELDS, FORECAST_DAYS,
            ),
        )
    }

    private fun get(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.setRequestProperty("User-Agent", "NosCorre (family organizer; https://github.com/ramonvidal/ChoreMe)")
        return try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Weather service answered ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    internal companion object {
        const val ENDPOINT = "https://api.open-meteo.com/v1/forecast"
        const val AIR_ENDPOINT = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val FORECAST_DAYS = 7
        const val AIR_DAYS = 3
        private const val TIMEOUT_MS = 10_000
        private val FRESH_FOR: Duration = Duration.ofHours(2)
        private val STALE_LIMIT: Duration = Duration.ofHours(12)

        /** About 1 km: enough for a forecast, too coarse to say which house. */
        fun coarse(point: GeoPoint) = GeoPoint(round2(point.latitude), round2(point.longitude))

        private fun round2(value: Double) = (value * 100).roundToLong() / 100.0

        fun key(point: GeoPoint): String = coarse(point).let { String.format(Locale.ROOT, "%.2f,%.2f", it.latitude, it.longitude) }
    }
}
