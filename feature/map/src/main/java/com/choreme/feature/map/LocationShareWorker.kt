package com.choreme.feature.map

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.location.DeviceLocation
import com.choreme.core.location.SharingChoice
import com.choreme.core.model.session.SessionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@HiltWorker
internal class LocationShareWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val sharing: SharingChoice,
    private val device: DeviceLocation,
    private val families: FamilyRepository,
    private val tracker: PresenceTracker,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Without "allow all the time" Android gives no position in the background; the
        // app still shares whenever the map is open.
        if (!availability.isConfigured || !sharing.isSharing.value || !device.hasBackgroundPermission()) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val fix = device.current() ?: return Result.success()
        return try {
            tracker.share(session.familyId, session.memberId, fix)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 30_000L
    }
}
