package com.choreme.core.calendar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which calendars this phone shows in the agenda. Calendar ids only mean something on the
 * phone that has them, so the choice is stored per phone, not shared with the family.
 * Null until the person has chosen.
 */
@Singleton
class CalendarChoice @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("agenda", Context.MODE_PRIVATE)
    private val _selected = MutableStateFlow(read())
    val selected: StateFlow<Set<Long>?> = _selected.asStateFlow()

    fun select(calendarIds: Set<Long>) {
        prefs.edit().putStringSet(KEY, calendarIds.map { it.toString() }.toSet()).apply()
        _selected.value = calendarIds
    }

    private fun read(): Set<Long>? = prefs.getStringSet(KEY, null)?.mapNotNull { it.toLongOrNull() }?.toSet()

    private companion object {
        const val KEY = "selected_calendars"
    }
}
