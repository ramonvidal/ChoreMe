package com.choreme.core.model.chore

import java.time.LocalDate

/**
 * The "our home" picture on the Chores tab: a house and garden that grows with the points the
 * family earned this week. Cooperative on purpose: everyone's chores grow the same garden.
 */
data class HomeScene(
    val points: Int,
    /** 0 (bare) to 1 (full bloom), from this week's points. */
    val growth: Float,
    /** Flowers in the garden, one per point up to [MAX_FLOWERS]. */
    val flowers: Int,
    /** The windows are lit when nothing is overdue. */
    val lightsOn: Boolean,
    /** A small cloud over the house while something is urgent. */
    val cloud: Boolean,
    /** The garden looks a little tired when chores pile up (never anything sad). */
    val tired: Boolean,
) {
    val fullBloom get() = growth >= 1f

    companion object {
        /** Points in a week for full bloom: about four chores a day for a family. */
        const val FULL_BLOOM_POINTS = 30
        const val MAX_FLOWERS = 12
        const val TIRED_AT_OVERDUE = 3
    }
}

fun homeScene(weekPoints: Int, overdue: Int, urgent: Int): HomeScene = HomeScene(
    points = weekPoints,
    growth = (weekPoints.toFloat() / HomeScene.FULL_BLOOM_POINTS).coerceIn(0f, 1f),
    flowers = weekPoints.coerceIn(0, HomeScene.MAX_FLOWERS),
    lightsOn = overdue == 0,
    cloud = urgent > 0,
    tired = overdue >= HomeScene.TIRED_AT_OVERDUE,
)

/** The scene for the family's [chores] today, after [weekPoints] earned in the last 7 days. */
fun homeScene(weekPoints: Int, chores: List<Chore>, today: LocalDate): HomeScene = homeScene(
    weekPoints = weekPoints,
    overdue = chores.count { it.isOverdue(today) },
    urgent = chores.count { it.urgent && it.isActive },
)
