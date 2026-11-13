package com.choreme.core.model.weather

import kotlin.math.max

/** How someone gets there: the same weather matters differently on a bike than in a car. */
enum class TripMode { WALK, BIKE, CAR, TRANSIT, OTHER }

/** What is worth knowing about the weather on a trip. */
enum class Concern { RAIN, HEAVY_RAIN, SNOW, ICE, FROST, COLD, HEAT, WIND, STORM, UV, FOG }

/** Things to bring or wear, at most two per trip. */
enum class Gear { UMBRELLA, RAIN_JACKET, WARM_JACKET, SUN_PROTECTION }

/** The weather on one trip, boiled down. [isQuiet] trips are not mentioned at all. */
data class Advice(
    val concerns: Set<Concern>,
    val gear: List<Gear>,
    val maxRainProbability: Int,
    val maxRainMm: Double,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val maxGustKmh: Double,
    /** Minutes to leave earlier because the way will be slow (heavy rain, snow, ice, fog by car or bus). */
    val leaveEarlierMinutes: Int,
) {
    val isQuiet: Boolean get() = concerns.isEmpty()

    /** The thing to say first: the one that matters most. */
    val headline: Concern? get() = PRIORITY.firstOrNull { it in concerns }
}

/** Most important first. */
private val PRIORITY = listOf(
    Concern.STORM, Concern.ICE, Concern.SNOW, Concern.HEAVY_RAIN, Concern.RAIN, Concern.WIND,
    Concern.FROST, Concern.HEAT, Concern.COLD, Concern.FOG, Concern.UV,
)

private val SNOW_CODES = setOf(71, 73, 75, 77, 85, 86)

/** Thresholds, a little lower for people outdoors (a bike or on foot). */
private class Limits(outdoors: Boolean, bike: Boolean) {
    val cold = if (bike) 8.0 else if (outdoors) 5.0 else 3.0
    val frost = if (outdoors) 2.0 else 1.0
    val heat = if (outdoors) 28.0 else 32.0
    val gust = if (bike) 35.0 else if (outdoors) 45.0 else 60.0
}

/**
 * What matters about [hours] (the hours of the whole trip, start to end, at every place it
 * touches) for someone travelling by [mode]. Pure and testable; the words come from the screen.
 */
fun adviceFor(hours: List<WeatherHour>, mode: TripMode): Advice {
    if (hours.isEmpty()) return Advice(emptySet(), emptyList(), 0, 0.0, 0.0, 0.0, 0.0, 0)
    val outdoors = mode == TripMode.WALK || mode == TripMode.BIKE
    val limits = Limits(outdoors, mode == TripMode.BIKE)
    val rainProbability = hours.maxOf { it.rainProbability }
    val rainMm = hours.maxOf { it.rainMm }
    val minTemp = hours.minOf { it.temperatureC }
    val maxTemp = hours.maxOf { it.temperatureC }
    val gust = hours.maxOf { max(it.gustKmh, it.windKmh) }
    val concerns = mutableSetOf<Concern>()

    val snow = hours.any { it.weatherCode in SNOW_CODES }
    val wetEnough = rainProbability >= 60 || (rainProbability >= 40 && rainMm >= 0.5)
    if (snow) concerns += Concern.SNOW
    if (wetEnough && !snow) concerns += Concern.RAIN
    if (wetEnough && rainMm >= 4.0) concerns += Concern.HEAVY_RAIN
    if (minTemp <= limits.frost) concerns += Concern.FROST
    // Ice: freezing and anything wet in the air, or fog that freezes.
    if (minTemp <= 0.0 && (wetEnough || hours.any { it.sky == Sky.DRIZZLE || it.sky == Sky.FOG })) concerns += Concern.ICE
    if (minTemp <= limits.cold && Concern.FROST !in concerns) concerns += Concern.COLD
    if (maxTemp >= limits.heat) concerns += Concern.HEAT
    if (gust >= limits.gust) concerns += Concern.WIND
    if (hours.any { it.sky == Sky.STORM }) concerns += Concern.STORM
    if (hours.any { it.isDay && it.uvIndex >= 6.0 }) concerns += Concern.UV
    if (hours.any { it.sky == Sky.FOG }) concerns += Concern.FOG

    val gear = buildList {
        if (Concern.RAIN in concerns) add(if (mode == TripMode.BIKE || Concern.HEAVY_RAIN in concerns && outdoors) Gear.RAIN_JACKET else Gear.UMBRELLA)
        if (Concern.COLD in concerns || Concern.FROST in concerns || Concern.SNOW in concerns) add(Gear.WARM_JACKET)
        if (Concern.UV in concerns || Concern.HEAT in concerns) add(Gear.SUN_PROTECTION)
    }.take(2)

    val slowWay = mode == TripMode.CAR || mode == TripMode.TRANSIT
    val leaveEarlier = when {
        !slowWay -> 0
        Concern.SNOW in concerns || Concern.ICE in concerns -> 15
        Concern.HEAVY_RAIN in concerns || Concern.FOG in concerns || Concern.STORM in concerns -> 10
        else -> 0
    }
    return Advice(concerns, gear, rainProbability, rainMm, minTemp, maxTemp, gust, leaveEarlier)
}
