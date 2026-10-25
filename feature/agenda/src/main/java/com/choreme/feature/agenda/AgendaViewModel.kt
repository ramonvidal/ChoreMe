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
import com.choreme.core.model.agenda.buildMonth
import com.choreme.core.model.agenda.monthGridRange
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
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

data class AgendaUiState(
    val hasPermission: Boolean = false,
    val calendars: List<FamilyCalendar> = emptyList(),
    /** Null until this phone has chosen which calendars to show. */
    val selected: Set<Long>? = null,
    val today: LocalDate = LocalDate.now(),
    val days: List<AgendaDay> = emptyList(),
    val view: AgendaView = AgendaView.LIST,
    val month: YearMonth = YearMonth.now(),
    val monthDays: Map<LocalDate, AgendaDay> = emptyMap(),
    val selectedDay: LocalDate = LocalDate.now(),
) {
    val selectedDayEntries get() = monthDays[selectedDay]
    val needsChoice get() = hasPermission && calendars.isNotEmpty() && selected == null
    val suggestion get() = suggestFamilyCalendars(calendars)
    val writableSelected get() = calendars.filter { it.isWritable && selected.orEmpty().contains(it.id) }
    fun calendarName(id: Long) = calendars.firstOrNull { it.id == id }?.name
}

enum class AgendaView { LIST, MONTH }

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val calendar: CalendarRepository,
    private val choice: CalendarChoice,
    families: FamilyRepository,
    chores: ChoreRepository,
) : ViewModel() {
    private val permission = MutableStateFlow(calendar.hasPermission())
    private val view = MutableStateFlow(AgendaView.LIST)
    private val month = MutableStateFlow(YearMonth.now())
    private val selectedDay = MutableStateFlow(LocalDate.now())

    private val calendars = permission.flatMapLatest { granted ->
        if (granted) calendar.observeCalendars() else flowOf(emptyList())
    }

    // Only the days on screen are read: the next two weeks, or the month grid.
    private val events = combine(permission, choice.selected, view, month) { granted, selected, mode, shownMonth ->
        EventWindow(granted, selected, mode, shownMonth)
    }
        .flatMapLatest { (granted, selected, mode, shownMonth) ->
            if (!granted || selected.isNullOrEmpty()) return@flatMapLatest flowOf(emptyList<CalendarEvent>())
            val zone = ZoneId.systemDefault()
            val (from, until) = when (mode) {
                AgendaView.LIST -> LocalDate.now().let { it to it.plusDays(DAYS.toLong()) }
                AgendaView.MONTH -> monthGridRange(shownMonth)
            }
            calendar.observeEvents(selected, from = from.atStartOfDay(zone).toInstant(), until = until.atStartOfDay(zone).toInstant())
        }

    private val familyChores = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { chores.observeChores(it.familyId) }
        .onStart { emit(emptyList()) }

    val uiState: StateFlow<AgendaUiState> = combine(
        combine(permission, calendars, choice.selected, ::Triple),
        events,
        familyChores,
        combine(view, month, selectedDay, ::Triple),
    ) { (granted, calendarList, selected), eventList, choreList, (mode, shownMonth, day) ->
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        AgendaUiState(
            hasPermission = granted,
            calendars = calendarList,
            selected = selected,
            today = today,
            days = if (mode == AgendaView.LIST) buildAgenda(eventList, choreList, from = today, days = DAYS, zone = zone) else emptyList(),
            view = mode,
            month = shownMonth,
            monthDays = if (mode == AgendaView.MONTH) buildMonth(eventList, choreList, shownMonth, today, zone) else emptyMap(),
            selectedDay = day,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AgendaUiState())

    fun showView(mode: AgendaView) {
        view.value = mode
    }

    fun showMonth(shown: YearMonth) {
        month.value = shown
        // Keep a selected day inside the month being looked at.
        if (YearMonth.from(selectedDay.value) != shown) {
            selectedDay.value = if (shown == YearMonth.now()) LocalDate.now() else shown.atDay(1)
        }
    }

    fun selectDay(day: LocalDate) {
        selectedDay.value = day
        if (YearMonth.from(day) != month.value) month.value = YearMonth.from(day)
    }

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

    private data class EventWindow(
        val granted: Boolean,
        val selected: Set<Long>?,
        val view: AgendaView,
        val month: YearMonth,
    )

    private companion object {
        /** How far ahead the agenda looks. */
        const val DAYS = 14
    }
}
