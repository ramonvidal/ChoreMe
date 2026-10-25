package com.choreme.core.push

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.unifiedpush.android.connector.UnifiedPush
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Connects this phone to a UnifiedPush distributor, an app such as ntfy that keeps one
 * battery-friendly connection for all apps. Free, no account, no server of ours.
 */
@Singleton
class PushRegistration @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("push", Context.MODE_PRIVATE)
    private val _endpoint = MutableStateFlow(prefs.getString(KEY_ENDPOINT, null))

    /** The address other phones ping this one at; null until a distributor gives one. */
    val endpoint: StateFlow<String?> = _endpoint.asStateFlow()

    /** Whether a distributor app such as ntfy is installed. */
    fun hasDistributor(): Boolean = isAvailable(context)

    /** Safe to call often; does nothing without a distributor. */
    fun register() {
        UnifiedPush.tryUseCurrentOrDefaultDistributor(context) { success ->
            if (success) UnifiedPush.register(context)
        }
    }

    internal fun onNewEndpoint(url: String) {
        prefs.edit().putString(KEY_ENDPOINT, url).apply()
        _endpoint.value = url
    }

    internal fun onUnregistered() {
        prefs.edit().remove(KEY_ENDPOINT).apply()
        _endpoint.value = null
    }

    companion object {
        private const val KEY_ENDPOINT = "endpoint"

        /** For code that runs before Hilt, such as scheduling at app start. */
        fun isAvailable(context: Context): Boolean = UnifiedPush.getDistributors(context).isNotEmpty()

        /** The free ntfy app on Google Play, the simplest distributor to install. */
        const val NTFY_PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=io.heckel.ntfy"
    }
}
