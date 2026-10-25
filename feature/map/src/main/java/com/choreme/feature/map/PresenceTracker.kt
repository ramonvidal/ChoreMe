package com.choreme.feature.map

import com.choreme.core.data.LocationRepository
import com.choreme.core.data.PushEndpointRepository
import com.choreme.core.location.DeviceFix
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.model.location.currentPlace
import com.choreme.core.model.location.placeEvents
import com.choreme.core.model.location.shouldPublish
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushSender
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
    private val endpoints: PushEndpointRepository,
    private val push: PushSender,
) {
    suspend fun share(familyId: String, memberId: String, fix: DeviceFix, places: List<Place>? = null) {
        val next = MemberLocation(memberId, fix.point, fix.accuracyMeters, fix.at)
        // Sitting still: no upload and no place check, which is most of the day for most phones.
        if (!shouldPublish(store.lastShared?.takeIf { it.memberId == memberId }, next)) return
        locations.publish(familyId, next)
        store.lastShared = next

        val known = places ?: locations.observePlaces(familyId).first()
        val previous = store.lastPlaceId?.let { id -> known.firstOrNull { it.id == id } }
        val now = currentPlace(previous, fix.point, fix.accuracyMeters, known)
        if (store.isPlaceKnown) {
            // A place deleted while someone was there has no "left" to announce: previous is null then.
            val events = placeEvents(memberId, previous, now, fix.at)
            locations.publishEvents(familyId, events)
            // Wake the other phones up so they show it now rather than at their next check.
            if (events.isNotEmpty()) push.ping(endpoints.othersThan(familyId, memberId), PushKinds.PLACE_EVENTS)
        }
        store.lastPlaceId = now?.id
    }

    /** After sharing stops (the shared position is deleted), the next share must go out. */
    fun forgetShared() {
        store.lastShared = null
    }
}
