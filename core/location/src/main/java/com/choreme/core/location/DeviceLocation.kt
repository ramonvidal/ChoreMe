package com.choreme.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.os.Build
import androidx.core.content.ContextCompat
import com.choreme.core.model.location.GeoPoint
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** A reading of where this phone is. */
data class DeviceFix(val point: GeoPoint, val accuracyMeters: Float, val at: Instant)

@Singleton
class DeviceLocation @Inject constructor(@ApplicationContext private val context: Context) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    /** Allowed while the app is open (precise or approximate). */
    fun hasPermission(): Boolean = FOREGROUND_PERMISSIONS.any(::isGranted)

    /** Allowed "all the time", so updates continue when the app is closed. */
    fun hasBackgroundPermission(): Boolean =
        hasPermission() && (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION))

    /**
     * A recent reading at battery-friendly accuracy (Wi-Fi and cell towers, GPS if needed),
     * or null without permission or if none comes in time. A fix Android already has from
     * the last few minutes, from any app, is reused: it costs no battery at all.
     */
    @SuppressLint("MissingPermission") // Checked by hasPermission().
    suspend fun current(): DeviceFix? {
        if (!hasPermission()) return null
        val last = runCatching { client.lastLocation.await() }.getOrNull()
        if (last != null && last.accuracy <= GOOD_ENOUGH_METERS &&
            Duration.between(Instant.ofEpochMilli(last.time), Instant.now()) <= FRESH_ENOUGH
        ) {
            return DeviceFix(GeoPoint(last.latitude, last.longitude), last.accuracy, Instant.ofEpochMilli(last.time))
        }
        val cancel = CancellationTokenSource()
        val location = withTimeoutOrNull(TIMEOUT_MS) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).await()
        }
        if (location == null) cancel.cancel()
        return location?.let { DeviceFix(GeoPoint(it.latitude, it.longitude), it.accuracy, Instant.ofEpochMilli(it.time)) }
    }

    /**
     * A steady stream of readings, at most one every [intervalMs] and only after moving
     * [minDistanceMeters]. Battery-friendly accuracy. For short stretches while someone is on the
     * move: it needs "allow all the time" (the caller checks) and stops when the flow is cancelled.
     */
    @SuppressLint("MissingPermission") // Checked by hasPermission().
    fun updates(intervalMs: Long, minDistanceMeters: Float): Flow<DeviceFix> = callbackFlow {
        if (!hasPermission()) {
            close()
            return@callbackFlow
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateDistanceMeters(minDistanceMeters)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach {
                    trySend(DeviceFix(GeoPoint(it.latitude, it.longitude), it.accuracy, Instant.ofEpochMilli(it.time)))
                }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose { client.removeLocationUpdates(callback) }
    }

    /**
     * Asks Android (which watches it cheaply, without this app running) to send [pending] once the
     * phone leaves a circle of [radiusMeters] around [center]. Replaces an earlier one for the same
     * [pending]. False when it couldn't be set (no permission, or Google Play services is busy).
     */
    @SuppressLint("MissingPermission") // Checked by hasBackgroundPermission().
    suspend fun notifyWhenLeaving(center: GeoPoint, radiusMeters: Float, pending: PendingIntent): Boolean {
        if (!hasBackgroundPermission()) return false
        val fence = Geofence.Builder()
            .setRequestId(FENCE_ID)
            .setCircularRegion(center.latitude, center.longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        val request = GeofencingRequest.Builder().addGeofence(fence).build()
        return runCatching { LocationServices.getGeofencingClient(context).addGeofences(request, pending).await() }.isSuccess
    }

    /** Takes back [notifyWhenLeaving]. */
    suspend fun stopNotifyingWhenLeaving(pending: PendingIntent) {
        runCatching { LocationServices.getGeofencingClient(context).removeGeofences(pending).await() }
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Whether [intent], sent by Android for [notifyWhenLeaving], says the phone left the circle. */
        fun isLeavingEvent(intent: Intent): Boolean {
            val event = GeofencingEvent.fromIntent(intent) ?: return false
            return !event.hasError() && event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        }

        val FOREGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        /** Has to be asked for on its own, after the foreground permission (Android 10+). */
        const val BACKGROUND_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION

        private const val FENCE_ID = "left-home-spot"
        private const val TIMEOUT_MS = 30_000L
        private const val GOOD_ENOUGH_METERS = 100f
        private val FRESH_ENOUGH: Duration = Duration.ofMinutes(5)
    }
}
