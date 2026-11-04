package com.choreme.feature.map

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.location.DeviceFix
import com.choreme.core.location.DeviceLocation
import com.choreme.core.location.SharingChoice
import com.choreme.core.model.location.MovementWatch
import com.choreme.core.model.session.SessionState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import javax.inject.Inject

/**
 * Shares this phone's position every few seconds while its owner is on the move, with a quiet
 * notification as Android requires. Once they've stayed in one place for a few minutes it sets
 * a new circle for [LiveShare] and stops, so a phone at rest costs almost nothing.
 */
@AndroidEntryPoint
internal class LiveShareService : Service() {
    @Inject lateinit var availability: FirebaseAvailability
    @Inject lateinit var sharing: SharingChoice
    @Inject lateinit var device: DeviceLocation
    @Inject lateinit var families: FamilyRepository
    @Inject lateinit var tracker: PresenceTracker

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android wants the notification within seconds of starting.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        running = true
        if (job?.isActive != true) job = scope.launch { follow() }
        return START_NOT_STICKY
    }

    private suspend fun follow() {
        val session = withTimeoutOrNull(SESSION_TIMEOUT_MS) { families.session.first { it != SessionState.Loading } } as? SessionState.InFamily
        if (session == null || !availability.isConfigured || !sharing.isSharing.value || !device.hasBackgroundPermission()) {
            stopSelf()
            return
        }
        val watch = MovementWatch(Instant.now())
        var last: DeviceFix? = null
        val readings = scope.launch {
            device.updates(UPDATE_EVERY_MS, MIN_DISTANCE_METERS).collect { fix ->
                last = fix
                watch.onFix(fix.point, fix.at)
                runCatching { tracker.share(session.familyId, session.memberId, fix) }
            }
        }
        while (scope.isActive && !watch.hasStopped(Instant.now()) && sharing.isSharing.value) delay(CHECK_EVERY_MS)
        readings.cancel()
        // Resting: watch a circle around here and let go of the position hardware.
        if (sharing.isSharing.value) LiveShare.arm(applicationContext, device, last?.point)
        stopSelf()
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.live_channel), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.live_channel_description) },
        )
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val open = launch?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_place)
            .setContentTitle(getString(R.string.live_title))
            .setContentText(getString(R.string.live_text))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        /** True while it's following someone, so the 15-minute check doesn't set a circle meanwhile. */
        @Volatile
        var running = false
            private set

        private const val CHANNEL_ID = "live-location"
        private const val NOTIFICATION_ID = 4101
        private const val SESSION_TIMEOUT_MS = 30_000L
        private const val UPDATE_EVERY_MS = 15_000L
        private const val MIN_DISTANCE_METERS = 20f
        private const val CHECK_EVERY_MS = 30_000L
    }
}
