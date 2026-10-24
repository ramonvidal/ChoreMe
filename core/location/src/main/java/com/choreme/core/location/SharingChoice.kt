package com.choreme.core.location

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Whether this phone shares its location with the family. Off until its owner turns it on. */
@Singleton
class SharingChoice @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)
    private val _isSharing = MutableStateFlow(prefs.getBoolean(KEY, false))
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    fun set(sharing: Boolean) {
        prefs.edit().putBoolean(KEY, sharing).apply()
        _isSharing.value = sharing
    }

    private companion object {
        const val KEY = "sharing"
    }
}
