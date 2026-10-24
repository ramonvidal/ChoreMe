package com.choreme.core.data

import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.ACCURACY
import com.choreme.core.data.remote.FirestoreSchema.AT
import com.choreme.core.data.remote.FirestoreSchema.LATITUDE
import com.choreme.core.data.remote.FirestoreSchema.LONGITUDE
import com.choreme.core.data.remote.FirestoreSchema.NAME
import com.choreme.core.data.remote.FirestoreSchema.RADIUS
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
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

    suspend fun addPlace(familyId: String, name: String, center: GeoPoint, radiusMeters: Int) {
        places(familyId).document()
            .set(mapOf(NAME to name, LATITUDE to center.latitude, LONGITUDE to center.longitude, RADIUS to radiusMeters.toLong()))
            .await()
    }

    suspend fun deletePlace(familyId: String, placeId: String) {
        places(familyId).document(placeId).delete().await()
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
}
