package com.choreme.feature.map

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.choreme.core.location.DeviceLocation
import com.choreme.core.model.location.GeoPoint

/**
 * Frequent location updates, only while someone is on the move. While they sit still, Android
 * quietly watches a circle around them (no battery to speak of). When they leave it, it starts
 * [LiveShareService], which shares often until they've stopped for a few minutes, then sets a
 * new circle and sleeps. Only runs while sharing is on and "allow all the time" is granted.
 */
internal object LiveShare {
    /** Wide enough that GPS wobble at home doesn't set it off. */
    private const val FENCE_RADIUS_METERS = 150f

    private fun leavingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LeftSpotReceiver::class.java),
        // Android adds what it found to the intent, so it has to be mutable.
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    /** Watches a circle around [at], or around where the phone is now. Safe to call often. */
    suspend fun arm(context: Context, device: DeviceLocation, at: GeoPoint? = null) {
        val center = at ?: device.current()?.point ?: return
        device.notifyWhenLeaving(center, FENCE_RADIUS_METERS, leavingIntent(context))
    }

    suspend fun disarm(context: Context, device: DeviceLocation) {
        device.stopNotifyingWhenLeaving(leavingIntent(context))
        context.stopService(Intent(context, LiveShareService::class.java))
    }
}
