package com.choreme.core.model.location

import java.time.Duration
import java.time.Instant
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(val latitude: Double, val longitude: Double)

/** Where a member's phone last said it was. Only exists while they share their location. */
data class MemberLocation(
    val memberId: String,
    val point: GeoPoint,
    val accuracyMeters: Float,
    val at: Instant,
)

/** A named place such as "Home" or "School", so the map can say "at School" rather than show coordinates. */
data class Place(
    val id: String,
    val name: String,
    val center: GeoPoint,
    val radiusMeters: Int,
) {
    companion object {
        val RADIUS_OPTIONS = listOf(100, 250, 500)
    }
}

/** Great-circle distance in meters. */
fun GeoPoint.distanceTo(other: GeoPoint): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(other.latitude - latitude)
    val dLon = Math.toRadians(other.longitude - longitude)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(latitude)) * cos(Math.toRadians(other.latitude)) * sin(dLon / 2).pow(2)
    return 2 * earthRadius * asin(sqrt(a))
}

/** The closest place the location falls inside, if any. */
fun MemberLocation.placeIn(places: List<Place>): Place? =
    places
        .map { it to point.distanceTo(it.center) }
        .filter { (place, distance) -> distance <= place.radiusMeters }
        .minByOrNull { it.second }
        ?.first

/** What the family sees for one member. */
sealed interface LocationStatus {
    data object NotSharing : LocationStatus

    data class Known(
        val location: MemberLocation,
        /** Null when they're not at any saved place. */
        val place: Place?,
        val age: Duration,
    ) : LocationStatus {
        /** Updates come every 15 minutes or so; much older than that means the phone is off or offline. */
        val isStale get() = age > STALE_AFTER
    }

    companion object {
        val STALE_AFTER: Duration = Duration.ofHours(1)
    }
}

fun locationStatus(location: MemberLocation?, places: List<Place>, now: Instant): LocationStatus =
    if (location == null) {
        LocationStatus.NotSharing
    } else {
        LocationStatus.Known(location, location.placeIn(places), Duration.between(location.at, now).coerceAtLeast(Duration.ZERO))
    }

/**
 * Whether a new reading is worth sending: the first one, after moving a bit, or as a
 * heartbeat now and then so "10 min ago" stays true when someone sits still.
 */
fun shouldPublish(previous: MemberLocation?, next: MemberLocation): Boolean =
    previous == null ||
        previous.point.distanceTo(next.point) >= MIN_MOVE_METERS ||
        Duration.between(previous.at, next.at) >= HEARTBEAT

private const val MIN_MOVE_METERS = 50.0
private val HEARTBEAT: Duration = Duration.ofMinutes(10)
