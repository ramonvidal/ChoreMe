package com.choreme.feature.map

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.MemberLocation
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

    /** The last position actually sent, so the same one isn't sent again (see shouldPublish). */
    var lastShared: MemberLocation?
        get() {
            if (!prefs.contains(KEY_SHARED_AT)) return null
            return MemberLocation(
                memberId = prefs.getString(KEY_SHARED_MEMBER, null) ?: return null,
                point = GeoPoint(prefs.getFloat(KEY_SHARED_LAT, 0f).toDouble(), prefs.getFloat(KEY_SHARED_LNG, 0f).toDouble()),
                accuracyMeters = 0f,
                at = Instant.ofEpochMilli(prefs.getLong(KEY_SHARED_AT, 0L)),
            )
        }
        set(value) {
            val edit = prefs.edit()
            if (value == null) {
                edit.remove(KEY_SHARED_MEMBER).remove(KEY_SHARED_LAT).remove(KEY_SHARED_LNG).remove(KEY_SHARED_AT)
            } else {
                edit.putString(KEY_SHARED_MEMBER, value.memberId)
                    .putFloat(KEY_SHARED_LAT, value.point.latitude.toFloat())
                    .putFloat(KEY_SHARED_LNG, value.point.longitude.toFloat())
                    .putLong(KEY_SHARED_AT, value.at.toEpochMilli())
            }
            edit.apply()
        }

    /** Events after this have not been shown yet. */
    var alertsCheckedUntil: Instant?
        get() = prefs.getLong(KEY_CHECKED, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        set(value) = prefs.edit().putLong(KEY_CHECKED, value?.toEpochMilli() ?: 0L).apply()

    /** Events already shown, so looking back a little further each time never repeats one. */
    var shownEventIds: Set<String>
        get() = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
        set(value) = prefs.edit().putStringSet(KEY_SHOWN, value).apply()

    private companion object {
        const val KEY_PLACE = "last_place"
        const val KEY_KNOWN = "place_known"
        const val KEY_CHECKED = "alerts_checked_until"
        const val KEY_SHOWN = "shown_events"
        const val KEY_SHARED_MEMBER = "shared_member"
        const val KEY_SHARED_LAT = "shared_lat"
        const val KEY_SHARED_LNG = "shared_lng"
        const val KEY_SHARED_AT = "shared_at"
    }
}
