package com.choreme.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Light or dark look: follow the phone, or the person's own choice. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The look this phone shows, picked under Family > This phone. Stored on the phone only. */
@Singleton
class AppearanceSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("appearance", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(read())
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _mode.value = mode
    }

    private fun read(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == prefs.getString(KEY_MODE, null) } ?: ThemeMode.SYSTEM

    private companion object {
        const val KEY_MODE = "theme_mode"
    }
}
