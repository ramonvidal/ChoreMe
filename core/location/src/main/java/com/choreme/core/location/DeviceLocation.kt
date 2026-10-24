package com.choreme.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.choreme.core.model.location.GeoPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
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
     * A fresh reading at battery-friendly accuracy (Wi-Fi and cell towers, GPS if needed),
     * or null without permission or if none comes in time.
     */
    @SuppressLint("MissingPermission") // Checked by hasPermission().
    suspend fun current(): DeviceFix? {
        if (!hasPermission()) return null
        val cancel = CancellationTokenSource()
        val location = withTimeoutOrNull(TIMEOUT_MS) {
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancel.token).await()
        }
        if (location == null) cancel.cancel()
        return location?.let { DeviceFix(GeoPoint(it.latitude, it.longitude), it.accuracy, Instant.ofEpochMilli(it.time)) }
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        val FOREGROUND_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

        /** Has to be asked for on its own, after the foreground permission (Android 10+). */
        const val BACKGROUND_PERMISSION = Manifest.permission.ACCESS_BACKGROUND_LOCATION

        private const val TIMEOUT_MS = 30_000L
    }
}
