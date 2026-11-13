package com.choreme.core.model.weather

/** Weather bad enough to tell everyone in the family, whatever their plan. */
enum class Extreme { STORM, HEAT_WAVE, HARD_FROST, GALE, TORRENTIAL_RAIN }

/** The extremes in [hours] (the next day or so at home). */
fun extremesIn(hours: List<WeatherHour>): Set<Extreme> = buildSet {
    if (hours.any { it.sky == Sky.STORM }) add(Extreme.STORM)
    if (hours.any { it.temperatureC >= HEAT_WAVE_C }) add(Extreme.HEAT_WAVE)
    if (hours.any { it.temperatureC <= HARD_FROST_C }) add(Extreme.HARD_FROST)
    if (hours.any { max(it.gustKmh, it.windKmh) >= GALE_KMH }) add(Extreme.GALE)
    if (hours.any { it.rainMm >= TORRENTIAL_MM }) add(Extreme.TORRENTIAL_RAIN)
}

private fun max(a: Double, b: Double) = if (a >= b) a else b

const val HEAT_WAVE_C = 35.0
const val HARD_FROST_C = -10.0
const val GALE_KMH = 80.0
const val TORRENTIAL_MM = 8.0
