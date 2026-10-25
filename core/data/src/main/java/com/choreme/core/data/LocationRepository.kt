package com.choreme.core.data

import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.ACCURACY
import com.choreme.core.data.remote.FirestoreSchema.AT
import com.choreme.core.data.remote.FirestoreSchema.LATITUDE
import com.choreme.core.data.remote.FirestoreSchema.LONGITUDE
import com.choreme.core.data.remote.FirestoreSchema.MEMBER_ID
import com.choreme.core.data.remote.FirestoreSchema.NAME
import com.choreme.core.data.remote.FirestoreSchema.PLACE_ID
import com.choreme.core.data.remote.FirestoreSchema.PLACE_NAME
import com.choreme.core.data.remote.FirestoreSchema.RADIUS
import com.choreme.core.data.remote.FirestoreSchema.TYPE
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.model.location.PlaceEvent
import com.choreme.core.model.location.PlaceEventType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Family members' shared locations and the family's named places. A member's location
 * document only exists while they share; stopping deletes it (see firestore.rules).
 */
@Singleton
class LocationRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun family(familyId: String) = firestore.get().collection(FirestoreSchema.FAMILIES).document(familyId)
    private fun locations(familyId: String) = family(familyId).collection(FirestoreSchema.LOCATIONS)
    private fun places(familyId: String) = family(familyId).collection(FirestoreSchema.PLACES)
    private fun placeEvents(familyId: String) = family(familyId).collection(FirestoreSchema.PLACE_EVENTS)

    fun observeLocations(familyId: String): Flow<List<MemberLocation>> =
        locations(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toMemberLocation() } }

    /** Waits for the server, so a background worker knows the update really went out. */
    suspend fun publish(familyId: String, location: MemberLocation) {
        locations(familyId).document(location.memberId)
            .set(
                mapOf(
                    LATITUDE to location.point.latitude,
                    LONGITUDE to location.point.longitude,
                    ACCURACY to location.accuracyMeters.toDouble(),
                    AT to location.at.toEpochMilli(),
                ),
            )
            .await()
    }

    suspend fun stopSharing(familyId: String, memberId: String) {
        locations(familyId).document(memberId).delete().await()
    }

    fun observePlaces(familyId: String): Flow<List<Place>> =
        places(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toPlace() }.sortedBy { it.name.lowercase() } }

    /**
     * Saves the place on the phone at once, so it shows up even with a weak signal.
     * Returns true once the server has it, false if it's still waiting for a connection.
     * Throws if the server refuses it.
     */
    suspend fun addPlace(familyId: String, name: String, center: GeoPoint, radiusMeters: Int): Boolean {
        val write = places(familyId).document()
            .set(mapOf(NAME to name, LATITUDE to center.latitude, LONGITUDE to center.longitude, RADIUS to radiusMeters.toLong()))
        return withTimeoutOrNull(SERVER_WAIT_MS) { write.await() } != null
    }

    suspend fun deletePlace(familyId: String, placeId: String) {
        places(familyId).document(placeId).delete().await()
    }

    suspend fun publishEvents(familyId: String, events: List<PlaceEvent>) {
        if (events.isEmpty()) return
        val batch = firestore.get().batch()
        events.forEach { event ->
            batch.set(
                placeEvents(familyId).document(),
                mapOf(
                    MEMBER_ID to event.memberId,
                    PLACE_ID to event.placeId,
                    PLACE_NAME to event.placeName,
                    TYPE to event.type.name,
                    AT to event.at.toEpochMilli(),
                ),
            )
        }
        batch.commit().await()
    }

    /** Events after [after], oldest first. */
    suspend fun eventsSince(familyId: String, after: Instant): List<PlaceEvent> =
        placeEvents(familyId)
            .whereGreaterThan(AT, after.toEpochMilli())
            .orderBy(AT, Query.Direction.ASCENDING)
            .get()
            .await()
            .documents
            .mapNotNull { it.toPlaceEvent() }

    /** Tidies up: each phone deletes its own member's old events. */
    suspend fun deleteOwnEventsBefore(familyId: String, memberId: String, before: Instant) {
        val old = placeEvents(familyId).whereLessThan(AT, before.toEpochMilli()).get().await().documents
            .filter { it.getString(MEMBER_ID) == memberId }
        if (old.isEmpty()) return
        val batch = firestore.get().batch()
        old.forEach { batch.delete(it.reference) }
        batch.commit().await()
    }

    private fun DocumentSnapshot.toPlaceEvent(): PlaceEvent? {
        val memberId = getString(MEMBER_ID) ?: return null
        val type = runCatching { PlaceEventType.valueOf(getString(TYPE).orEmpty()) }.getOrNull() ?: return null
        val at = getLong(AT) ?: return null
        return PlaceEvent(memberId, getString(PLACE_ID).orEmpty(), getString(PLACE_NAME).orEmpty(), type, Instant.ofEpochMilli(at), id)
    }

    private fun DocumentSnapshot.point(): GeoPoint? {
        val latitude = getDouble(LATITUDE) ?: return null
        val longitude = getDouble(LONGITUDE) ?: return null
        return GeoPoint(latitude, longitude)
    }

    private fun DocumentSnapshot.toMemberLocation(): MemberLocation? {
        val point = point() ?: return null
        val at = getLong(AT) ?: return null
        return MemberLocation(id, point, accuracyMeters = getDouble(ACCURACY)?.toFloat() ?: 0f, at = Instant.ofEpochMilli(at))
    }

    private fun DocumentSnapshot.toPlace(): Place? {
        val name = getString(NAME) ?: return null
        val center = point() ?: return null
        val radius = getLong(RADIUS)?.toInt() ?: return null
        return Place(id, name, center, radius)
    }

    private companion object {
        const val SERVER_WAIT_MS = 10_000L
    }
}
