package com.choreme.feature.map

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** The phone left the circle [LiveShare] watched: time to share often. */
internal class LeftSpotReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError() || event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_EXIT) return
        // Android lets a geofence start a foreground service from the background.
        ContextCompat.startForegroundService(context, Intent(context, LiveShareService::class.java))
    }
}
