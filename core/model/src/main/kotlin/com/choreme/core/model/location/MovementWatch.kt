package com.choreme.core.model.location

import java.time.Duration
import java.time.Instant

/**
 * Tells when someone who was on the move has stopped, so frequent location updates (which cost
 * battery) can go back to sleep. Feed it every reading: it counts as moving whenever a
 * reading is [MOVED_METERS] or more from the last place it counted, and as stopped once
 * [STILL_FOR] has passed without that. Starts "moving" at [startedAt].
 */
class MovementWatch(startedAt: Instant) {
    private var anchor: GeoPoint? = null
    private var movedAt: Instant = startedAt

    fun onFix(point: GeoPoint, at: Instant) {
        val from = anchor
        if (from == null) {
            anchor = point
        } else if (from.distanceTo(point) >= MOVED_METERS) {
            anchor = point
            movedAt = at
        }
    }

    fun hasStopped(now: Instant): Boolean = Duration.between(movedAt, now) >= STILL_FOR

    companion object {
        /** Further than GPS wobble at balanced accuracy, closer than a few steps down the street. */
        const val MOVED_METERS = 75.0
        val STILL_FOR: Duration = Duration.ofMinutes(5)
    }
}
