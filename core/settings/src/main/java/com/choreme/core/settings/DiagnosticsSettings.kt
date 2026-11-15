package com.choreme.core.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Whether this phone sends anonymous crash reports (so crashes can be fixed). On unless the person turns it off. */
@Singleton
class DiagnosticsSettings @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("diagnostics", Context.MODE_PRIVATE)
    private val _crashReports = MutableStateFlow(prefs.getBoolean(KEY, true))
    val crashReports: StateFlow<Boolean> = _crashReports.asStateFlow()

    fun setCrashReports(on: Boolean) {
        prefs.edit().putBoolean(KEY, on).apply()
        _crashReports.value = on
    }

    private companion object {
        const val KEY = "crash_reports"
    }
}
