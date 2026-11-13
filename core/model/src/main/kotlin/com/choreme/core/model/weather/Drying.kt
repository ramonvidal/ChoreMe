package com.choreme.core.model.weather

/** How good a day is for drying washing outside. */
enum class DryingQuality { GOOD, OK, POOR }

/**
 * From the daytime [hours] (say 9:00 to 17:00): dry air, some wind, mild warmth and no rain make a good
 * day. Returns [DryingQuality.POOR] with no forecast.
 */
fun dryingQuality(hours: List<WeatherHour>): DryingQuality {
    if (hours.isEmpty()) return DryingQuality.POOR
    val rain = hours.maxOf { it.rainProbability }
    val rainMm = hours.sumOf { it.rainMm }
    val humidity = hours.map { it.humidity }.average()
    val temp = hours.map { it.temperatureC }.average()
    val wind = hours.map { it.windKmh }.average()
    return when {
        rain >= 50 || rainMm >= 1.0 || temp < 5.0 -> DryingQuality.POOR
        rain < 20 && humidity <= 70 && temp >= 12.0 && wind >= 6.0 -> DryingQuality.GOOD
        rain < 35 && humidity <= 85 && temp >= 8.0 -> DryingQuality.OK
        else -> DryingQuality.POOR
    }
}
