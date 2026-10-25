package com.choreme.feature.map

import com.choreme.core.data.LocationRepository
import com.choreme.core.location.DeviceFix
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.model.location.currentPlace
import com.choreme.core.model.location.placeEvents
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shares a new position and, when it means arriving at or leaving a saved place, records
 * that for the rest of the family. Used by both the map and the background worker.
 */
@Singleton
internal class PresenceTracker @Inject constructor(
    private val locations: LocationRepository,
    private val store: PresenceStore,
) {
    suspend fun share(familyId: String, memberId: String, fix: DeviceFix, places: List<Place>? = null) {
        locations.publish(familyId, MemberLocation(memberId, fix.point, fix.accuracyMeters, fix.at))

        val known = places ?: locations.observePlaces(familyId).first()
        val previous = store.lastPlaceId?.let { id -> known.firstOrNull { it.id == id } }
        val now = currentPlace(previous, fix.point, fix.accuracyMeters, known)
        if (store.isPlaceKnown) {
            // A place deleted while someone was there has no "left" to announce: previous is null then.
            locations.publishEvents(familyId, placeEvents(memberId, previous, now, fix.at))
        }
        store.lastPlaceId = now?.id
    }
}
