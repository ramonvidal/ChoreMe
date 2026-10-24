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
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.model.location.locationStatus
import com.choreme.core.model.location.shouldPublish
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
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
) {
    val isParent get() = me?.role == Role.PARENT
}

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    families: FamilyRepository,
    private val locations: LocationRepository,
    private val device: DeviceLocation,
    private val sharing: SharingChoice,
) : ViewModel() {
    private val permissions = MutableStateFlow(device.hasPermission() to device.hasBackgroundPermission())
    private val myFix = MutableStateFlow<DeviceFix?>(null)

    val uiState: StateFlow<MapUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            combine(
                families.observeMembers(session.familyId),
                locations.observeLocations(session.familyId),
                locations.observePlaces(session.familyId),
                combine(sharing.isSharing, permissions, myFix) { on, perms, fix -> Triple(on, perms, fix) },
            ) { members, locationList, places, (isSharing, perms, fix) ->
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
            val next = MemberLocation(me.id, fix.point, fix.accuracyMeters, fix.at)
            val previous = (state.members.firstOrNull { it.member.id == me.id }?.status as? LocationStatus.Known)?.location
            if (!shouldPublish(previous, next)) return@launch
            _actionState.value = try {
                locations.publish(familyId, next)
                ActionState()
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't share location", e)
                ActionState(error = R.string.share_failed)
            }
        }
    }

    fun addPlace(name: String, radiusMeters: Int) {
        val familyId = uiState.value.familyId ?: return
        val fix = myFix.value ?: return
        runAction(_actionState) { locations.addPlace(familyId, name.trim(), fix.point, radiusMeters) }
    }

    fun deletePlace(place: Place) {
        val familyId = uiState.value.familyId ?: return
        runAction(_actionState) { locations.deletePlace(familyId, place.id) }
    }
}
