package com.choreme.core.model.chore

import java.time.LocalDate

/**
 * The "our home" picture on the Chores tab: a house and garden that changes with every chore.
 * Adding one puts a weed in the grass; finishing it pulls the weed and grows flowers (its points,
 * over the last 7 days, so there's no Monday reset). The more chores wait, the worse the weather:
 * clouds, then rain, then a storm. Cooperative on purpose: one garden for all.
 */
data class HomeScene(
    val points: Int,
    /** 0 (bare) to 1 (full bloom), from the last 7 days' points. */
    val growth: Float,
    /** Flowers in the garden, one per point up to [MAX_FLOWERS]. */
    val flowers: Int,
    /** One per chore waiting to be done today (up to [MAX_WEEDS]). */
    val weeds: Int = 0,
    /** How many of the weeds are overgrown: chores past their day. */
    val overgrown: Int = 0,
    val weather: Weather = Weather.SUNNY,
    /** The windows are lit when nothing is overdue. */
    val lightsOn: Boolean,
    /** The garden looks a little tired when chores pile up (never anything sad). */
    val tired: Boolean,
) {
    val fullBloom get() = growth >= 1f

    companion object {
        /** Points in a week for full bloom: about four chores a day for a family. */
        const val FULL_BLOOM_POINTS = 30
        const val MAX_FLOWERS = 12
        const val MAX_WEEDS = 10
        const val TIRED_AT_OVERDUE = 3
        /** Weather "pressure" (chores waiting, plus overdue and urgent ones again) for each step. */
        const val CLOUDY_AT = 3
        const val RAIN_AT = 6
        const val STORM_AT = 10
    }
}

/** Sunny while little waits; clouds, rain and then a storm as chores pile up. */
enum class Weather { SUNNY, CLOUDY, RAIN, STORM }

/**
 * Chores waiting today count once, overdue and urgent ones twice. Anything urgent brings at
 * least a cloud.
 */
fun weatherFor(toDo: Int, overdue: Int, urgent: Int): Weather {
    val pressure = toDo + overdue + urgent
    return when {
        pressure >= HomeScene.STORM_AT -> Weather.STORM
        pressure >= HomeScene.RAIN_AT -> Weather.RAIN
        pressure >= HomeScene.CLOUDY_AT || urgent > 0 -> Weather.CLOUDY
        else -> Weather.SUNNY
    }
}

fun homeScene(weekPoints: Int, overdue: Int, urgent: Int, toDo: Int = 0): HomeScene = HomeScene(
    points = weekPoints,
    growth = (weekPoints.toFloat() / HomeScene.FULL_BLOOM_POINTS).coerceIn(0f, 1f),
    flowers = weekPoints.coerceIn(0, HomeScene.MAX_FLOWERS),
    weeds = toDo.coerceIn(0, HomeScene.MAX_WEEDS),
    overgrown = overdue.coerceIn(0, toDo.coerceIn(0, HomeScene.MAX_WEEDS)),
    weather = weatherFor(toDo, overdue, urgent),
    lightsOn = overdue == 0,
    tired = overdue >= HomeScene.TIRED_AT_OVERDUE,
)

/** The scene for the family's [chores] today, after [weekPoints] earned in the last 7 days. */
fun homeScene(weekPoints: Int, chores: List<Chore>, today: LocalDate): HomeScene = homeScene(
    weekPoints = weekPoints,
    overdue = chores.count { it.isOverdue(today) },
    urgent = chores.count { it.urgent && it.isActive },
    toDo = chores.count { it.isActive && !it.dueDate.isAfter(today) },
)
