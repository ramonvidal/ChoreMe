package com.choreme.feature.agenda

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.calendar.CalendarChoice
import com.choreme.core.calendar.CalendarRepository
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.model.agenda.AgendaDay
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.agenda.FamilyCalendar
import com.choreme.core.model.agenda.NewEvent
import com.choreme.core.model.agenda.buildAgenda
import com.choreme.core.model.agenda.suggestFamilyCalendars
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.runAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AgendaUiState(
    val hasPermission: Boolean = false,
    val calendars: List<FamilyCalendar> = emptyList(),
    /** Null until this phone has chosen which calendars to show. */
    val selected: Set<Long>? = null,
    val today: LocalDate = LocalDate.now(),
    val days: List<AgendaDay> = emptyList(),
) {
    val needsChoice get() = hasPermission && calendars.isNotEmpty() && selected == null
    val suggestion get() = suggestFamilyCalendars(calendars)
    val writableSelected get() = calendars.filter { it.isWritable && selected.orEmpty().contains(it.id) }
    fun calendarName(id: Long) = calendars.firstOrNull { it.id == id }?.name
}

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val calendar: CalendarRepository,
    private val choice: CalendarChoice,
    families: FamilyRepository,
    chores: ChoreRepository,
) : ViewModel() {
    private val permission = MutableStateFlow(calendar.hasPermission())

    private val calendars = permission.flatMapLatest { granted ->
        if (granted) calendar.observeCalendars() else flowOf(emptyList())
    }

    private val events = combine(permission, choice.selected) { granted, selected -> granted to selected }
        .flatMapLatest { (granted, selected) ->
            if (!granted || selected.isNullOrEmpty()) return@flatMapLatest flowOf(emptyList<CalendarEvent>())
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            calendar.observeEvents(
                selected,
                from = today.atStartOfDay(zone).toInstant(),
                until = today.plusDays(DAYS.toLong()).atStartOfDay(zone).toInstant(),
            )
        }

    private val familyChores = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { chores.observeChores(it.familyId) }
        .onStart { emit(emptyList()) }

    val uiState: StateFlow<AgendaUiState> = combine(permission, calendars, choice.selected, events, familyChores) {
            granted, calendarList, selected, eventList, choreList ->
        val today = LocalDate.now()
        AgendaUiState(
            hasPermission = granted,
            calendars = calendarList,
            selected = selected,
            today = today,
            days = buildAgenda(eventList, choreList, from = today, days = DAYS, zone = ZoneId.systemDefault()),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    private val _actionState = MutableStateFlow(ActionState())
    val actionState: StateFlow<ActionState> = _actionState

    /** Called after the permission dialog closes, whatever the answer. */
    fun onPermissionResult() {
        permission.value = calendar.hasPermission()
    }

    fun choose(calendarIds: Set<Long>) = choice.select(calendarIds)

    fun addEvent(calendarId: Long, event: NewEvent) = runAction(_actionState) {
        calendar.add(calendarId, event.copy(title = event.title.trim()))
    }

    fun openIntent(event: CalendarEvent): Intent = calendar.openIntent(event)

    private companion object {
        /** How far ahead the agenda looks. */
        const val DAYS = 14
    }
}
