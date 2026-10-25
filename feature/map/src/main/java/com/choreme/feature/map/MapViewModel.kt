package com.choreme.feature.map

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.LocationRepository
import com.choreme.core.location.DeviceFix
import com.choreme.core.location.DeviceLocation
import com.choreme.core.location.SharingChoice
import com.choreme.core.push.PushRegistration
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.Place
import com.choreme.core.model.location.locationStatus
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class MemberOnMap(val member: Member, val status: LocationStatus)

data class MapUiState(
    val loading: Boolean = true,
    val familyId: String? = null,
    val me: Member? = null,
    val members: List<MemberOnMap> = emptyList(),
    val places: List<Place> = emptyList(),
    val isSharing: Boolean = false,
    val hasPermission: Boolean = false,
    val hasBackgroundPermission: Boolean = false,
    /** This phone's latest reading, for centering the map and saving a place. */
    val myFix: DeviceFix? = null,
    val alertsEnabled: Boolean = true,
    val instant: InstantAlerts = InstantAlerts(hasApp = false, connected = false),
) {
    val isParent get() = me?.role == Role.PARENT
}

sealed interface PlaceSaved {
    data class Saved(val name: String) : PlaceSaved

    /** Saved on this phone; it reaches the family once there's a connection. */
    data class Pending(val name: String) : PlaceSaved
    data object Failed : PlaceSaved
}

private data class Settings(
    val isSharing: Boolean,
    val permissions: Pair<Boolean, Boolean>,
    val fix: DeviceFix?,
    val alertsEnabled: Boolean,
    val instant: InstantAlerts,
)

/** Instant alerts through a UnifiedPush app such as ntfy. */
data class InstantAlerts(val hasApp: Boolean, val connected: Boolean)

@HiltViewModel
class MapViewModel @Inject internal constructor(
    @ApplicationContext private val context: Context,
    families: FamilyRepository,
    private val locations: LocationRepository,
    private val device: DeviceLocation,
    private val sharing: SharingChoice,
    private val tracker: PresenceTracker,
    private val presence: PresenceStore,
    private val push: PushRegistration,
) : ViewModel() {
    private val hasPushApp = MutableStateFlow(push.hasDistributor())
    private val permissions = MutableStateFlow(device.hasPermission() to device.hasBackgroundPermission())
    private val myFix = MutableStateFlow<DeviceFix?>(null)

    val uiState: StateFlow<MapUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            combine(
                families.observeMembers(session.familyId),
                locations.observeLocations(session.familyId),
                locations.observePlaces(session.familyId),
                combine(sharing.isSharing, permissions, myFix, presence.alertsEnabled, combine(hasPushApp, push.endpoint, ::Pair)) {
                        on, perms, fix, alerts, (pushApp, endpoint) ->
                    Settings(on, perms, fix, alerts, InstantAlerts(pushApp, endpoint != null))
                },
            ) { members, locationList, places, settings ->
                val (isSharing, perms, fix, alerts, instant) = settings
                val byMember = locationList.associateBy { it.memberId }
                val now = Instant.now()
                MapUiState(
                    loading = false,
                    familyId = session.familyId,
                    me = members.firstOrNull { it.id == session.memberId } ?: Member(session.memberId, "", session.role),
                    members = members.map { MemberOnMap(it, locationStatus(byMember[it.id], places, now)) },
                    places = places,
                    isSharing = isSharing,
                    hasPermission = perms.first,
                    hasBackgroundPermission = perms.second,
                    myFix = fix,
                    alertsEnabled = alerts,
                    instant = instant,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MapUiState())

    private val _actionState = MutableStateFlow(ActionState())
    val actionState: StateFlow<ActionState> = _actionState

    /** After any permission dialog; also refreshes this phone's position. */
    fun onPermissionsChanged() {
        permissions.value = device.hasPermission() to device.hasBackgroundPermission()
        refresh()
    }

    fun setSharing(on: Boolean) {
        sharing.set(on)
        if (on) {
            LocationSharing.start(context)
            refresh()
        } else {
            LocationSharing.stop(context)
            tracker.forgetShared()
            val state = uiState.value
            val familyId = state.familyId ?: return
            val memberId = state.me?.id ?: return
            runAction(_actionState) { locations.stopSharing(familyId, memberId) }
        }
    }

    /**
     * Gets a fresh position and, when sharing, sends it if it's worth sending. Waits for the
     * family to load first, so opening the map shares right away instead of skipping it.
     */
    fun refresh() {
        viewModelScope.launch {
            val fix = device.current()
            if (fix == null) {
                if (device.hasPermission()) _actionState.value = ActionState(error = R.string.no_position)
                return@launch
            }
            myFix.value = fix
            if (!sharing.isSharing.value) return@launch
            val state = uiState.first { !it.loading }
            val familyId = state.familyId ?: return@launch
            val me = state.me ?: return@launch
            // The family can't see this phone right now (e.g. a parent tidied up): send it regardless.
            val visible = state.members.firstOrNull { it.member.id == me.id }?.status is LocationStatus.Known
            if (!visible) tracker.forgetShared()
            _actionState.value = try {
                tracker.share(familyId, me.id, fix, state.places)
                ActionState()
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't share location", e)
                ActionState(error = R.string.share_failed)
            }
        }
    }

    private val _placeSaved = MutableSharedFlow<PlaceSaved>(extraBufferCapacity = 1)

    /** How saving a place went, for a short message on screen. */
    val placeSaved: SharedFlow<PlaceSaved> = _placeSaved

    fun addPlace(name: String, radiusMeters: Int) {
        val familyId = uiState.value.familyId ?: return
        val fix = myFix.value ?: return
        val trimmed = name.trim()
        viewModelScope.launch {
            val result = try {
                if (locations.addPlace(familyId, trimmed, fix.point, radiusMeters)) PlaceSaved.Saved(trimmed) else PlaceSaved.Pending(trimmed)
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't save place", e)
                PlaceSaved.Failed
            }
            _placeSaved.emit(result)
        }
    }

    fun setAlertsEnabled(enabled: Boolean) = presence.setAlertsEnabled(enabled)

    /** When the map is shown again, e.g. after installing ntfy. */
    fun onResume() {
        hasPushApp.value = push.hasDistributor()
        push.register()
    }

    fun deletePlace(place: Place) {
        val familyId = uiState.value.familyId ?: return
        runAction(_actionState) { locations.deletePlace(familyId, place.id) }
    }
}
