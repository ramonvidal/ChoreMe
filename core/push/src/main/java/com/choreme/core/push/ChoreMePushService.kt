package com.choreme.core.push

import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import org.unifiedpush.android.connector.FailedReason
import org.unifiedpush.android.connector.PushService
import org.unifiedpush.android.connector.data.PushEndpoint
import org.unifiedpush.android.connector.data.PushMessage
import javax.inject.Inject

@AndroidEntryPoint
internal class ChoreMePushService : PushService() {
    @Inject lateinit var registration: PushRegistration
    @Inject lateinit var listeners: Set<@JvmSuppressWildcards PushListener>

    override fun onNewEndpoint(endpoint: PushEndpoint, instance: String) {
        registration.onNewEndpoint(endpoint.url)
    }

    override fun onMessage(message: PushMessage, instance: String) {
        val kind = message.content.decodeToString().trim()
        listeners.forEach { it.onPing(kind) }
    }

    override fun onRegistrationFailed(reason: FailedReason, instance: String) {
        Log.w("ChoreMe", "Push registration failed: $reason")
    }

    override fun onUnregistered(instance: String) {
        registration.onUnregistered()
    }
}
