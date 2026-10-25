package com.choreme.core.settings

import android.content.Context
import androidx.core.app.NotificationCompat
import com.choreme.core.model.AlertKind
import com.choreme.core.model.AlertSettings
import com.choreme.core.model.QuietHours
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Which notifications this phone shows and when to keep them quiet. Stored on the phone only. */
@Singleton
class NotificationSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("notifications", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AlertSettings> = _settings.asStateFlow()

    fun isEnabled(kind: AlertKind): Boolean = settings.value.isEnabled(kind)

    fun setEnabled(kind: AlertKind, enabled: Boolean) {
        val disabled = if (enabled) settings.value.disabled - kind else settings.value.disabled + kind
        save(settings.value.copy(disabled = disabled))
    }

    fun setQuietHours(quietHours: QuietHours?) = save(settings.value.copy(quietHours = quietHours))

    /** Makes a notification silent during quiet hours; it still shows, just without sound. */
    fun applyQuietHours(builder: NotificationCompat.Builder, now: LocalTime = LocalTime.now()): NotificationCompat.Builder =
        builder.setSilent(settings.value.isSilent(now))

    private fun save(value: AlertSettings) {
        prefs.edit()
            .putStringSet(KEY_DISABLED, value.disabled.map { it.name }.toSet())
            .putBoolean(KEY_QUIET, value.quietHours != null)
            .apply {
                value.quietHours?.let {
                    putInt(KEY_QUIET_START, it.start.toSecondOfDay() / 60)
                    putInt(KEY_QUIET_END, it.end.toSecondOfDay() / 60)
                }
            }
            .apply()
        _settings.value = value
    }

    private fun read(): AlertSettings {
        val disabled = prefs.getStringSet(KEY_DISABLED, emptySet()).orEmpty()
            .mapNotNull { name -> AlertKind.entries.firstOrNull { it.name == name } }
            .toSet()
        val quiet = if (prefs.getBoolean(KEY_QUIET, false)) {
            QuietHours(
                LocalTime.ofSecondOfDay(prefs.getInt(KEY_QUIET_START, QuietHours.DEFAULT.start.toSecondOfDay() / 60) * 60L),
                LocalTime.ofSecondOfDay(prefs.getInt(KEY_QUIET_END, QuietHours.DEFAULT.end.toSecondOfDay() / 60) * 60L),
            )
        } else {
            null
        }
        return AlertSettings(disabled, quiet)
    }

    private companion object {
        const val KEY_DISABLED = "disabled"
        const val KEY_QUIET = "quiet"
        const val KEY_QUIET_START = "quiet_start_minute"
        const val KEY_QUIET_END = "quiet_end_minute"
    }
}
