package com.choreme.feature.map

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** What this phone remembers between location checks; nothing here is shared. */
@Singleton
internal class PresenceStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("presence", Context.MODE_PRIVATE)

    /** The saved place this phone was last at; null when it's at none. */
    var lastPlaceId: String?
        get() = prefs.getString(KEY_PLACE, null)
        set(value) = prefs.edit().putString(KEY_PLACE, value).putBoolean(KEY_KNOWN, true).apply()

    /** False until the first position, so installing the app doesn't announce "arrived at Home". */
    val isPlaceKnown: Boolean get() = prefs.getBoolean(KEY_KNOWN, false)

    /** Events after this have not been shown yet. */
    var alertsCheckedUntil: Instant?
        get() = prefs.getLong(KEY_CHECKED, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        set(value) = prefs.edit().putLong(KEY_CHECKED, value?.toEpochMilli() ?: 0L).apply()

    /** Events already shown, so looking back a little further each time never repeats one. */
    var shownEventIds: Set<String>
        get() = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_SHOWN, value).apply()

    private val _alertsEnabled = MutableStateFlow(prefs.getBoolean(KEY_ALERTS, true))
    val alertsEnabled: StateFlow<Boolean> = _alertsEnabled.asStateFlow()

    fun setAlertsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ALERTS, enabled).apply()
        _alertsEnabled.value = enabled
    }

    private companion object {
        const val KEY_PLACE = "last_place"
        const val KEY_KNOWN = "place_known"
        const val KEY_CHECKED = "alerts_checked_until"
        const val KEY_ALERTS = "alerts_enabled"
        const val KEY_SHOWN = "shown_events"
    }
}
