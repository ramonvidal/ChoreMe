package com.choreme.core.model.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class LocationTest {
    private val home = Place("home", "Home", GeoPoint(41.3874, 2.1686), radiusMeters = 100)
    private val school = Place("school", "School", GeoPoint(41.3950, 2.1600), radiusMeters = 250)
    private val now = Instant.parse("2026-10-25T10:00:00Z")

    private fun at(point: GeoPoint, minutesAgo: Long = 0) =
        MemberLocation("leo", point, accuracyMeters = 20f, at = now.minus(Duration.ofMinutes(minutesAgo)))

    @Test
    fun `distance is in meters`() {
        // Barcelona to Madrid is about 505 km.
        val distance = GeoPoint(41.3874, 2.1686).distanceTo(GeoPoint(40.4168, -3.7038))
        assertEquals(505_000.0, distance, 5_000.0)
    }

    @Test
    fun `a location inside a place's radius is at that place`() {
        assertEquals(home, at(GeoPoint(41.3878, 2.1690)).placeIn(listOf(home, school)))
        assertNull(at(GeoPoint(41.40, 2.20)).placeIn(listOf(home, school)))
    }

    @Test
    fun `overlapping places pick the closest`() {
        val bigHome = home.copy(radiusMeters = 2_000)
        assertEquals(school, at(school.center).placeIn(listOf(bigHome, school)))
    }

    @Test
    fun `status says where and how long ago`() {
        val status = locationStatus(at(home.center, minutesAgo = 5), listOf(home), now) as LocationStatus.Known
        assertEquals(home, status.place)
        assertEquals(Duration.ofMinutes(5), status.age)
        assertFalse(status.isStale)
        assertTrue((locationStatus(at(home.center, minutesAgo = 90), listOf(home), now) as LocationStatus.Known).isStale)
        assertEquals(LocationStatus.NotSharing, locationStatus(null, listOf(home), now))
    }

    @Test
    fun `readings are sent when moving or as a heartbeat, not on every tick`() {
        val last = at(home.center, minutesAgo = 3)
        assertTrue(shouldPublish(null, at(home.center)))
        assertFalse(shouldPublish(last, at(GeoPoint(41.38745, 2.16865))))
        assertTrue(shouldPublish(last, at(school.center)))
        assertFalse(shouldPublish(at(home.center, minutesAgo = 12), at(home.center)))
        assertTrue(shouldPublish(at(home.center, minutesAgo = 31), at(home.center)))
    }
}

class PlaceEventTest {
    private val home = Place("home", "Home", GeoPoint(41.3874, 2.1686), radiusMeters = 100)
    private val school = Place("school", "School", GeoPoint(41.3950, 2.1600), radiusMeters = 250)
    private val places = listOf(home, school)
    private val now = Instant.parse("2026-10-25T10:00:00Z")

    // About 120 m north of home: outside its 100 m, but inside the leaving margin.
    private val justOutsideHome = GeoPoint(41.38848, 2.1686)

    @Test
    fun `jitter at the edge doesn't count as leaving`() {
        assertEquals(home, currentPlace(previous = home, point = justOutsideHome, accuracyMeters = 10f, places = places))
        // Arriving from outside still needs to be really inside.
        assertNull(currentPlace(previous = null, point = justOutsideHome, accuracyMeters = 10f, places = places))
    }

    @Test
    fun `walking from home to school leaves one and arrives at the other`() {
        val at = currentPlace(previous = home, point = school.center, accuracyMeters = 10f, places = places)
        assertEquals(school, at)
        assertEquals(
            listOf(PlaceEventType.LEFT to "Home", PlaceEventType.ARRIVED to "School"),
            placeEvents("leo", from = home, to = at, at = now).map { it.type to it.placeName },
        )
    }

    @Test
    fun `staying put or a deleted place sends nothing odd`() {
        assertEquals(emptyList<PlaceEvent>(), placeEvents("leo", home, home, now))
        // The place was deleted while they were there: they're simply nowhere now, no alert needed beyond "left".
        assertNull(currentPlace(previous = home, point = home.center, accuracyMeters = 10f, places = listOf(school)))
    }
}
