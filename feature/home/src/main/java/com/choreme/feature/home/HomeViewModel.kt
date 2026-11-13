package com.choreme.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.calendar.CalendarChoice
import com.choreme.core.calendar.CalendarRepository
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.GroceryRepository
import com.choreme.core.data.LocationRepository
import com.choreme.core.data.PlanRepository
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.groceriesToBuy
import com.choreme.core.model.home.DayTimeline
import com.choreme.core.model.home.dayTimeline
import com.choreme.core.model.location.Place
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.settings.WeatherSettings
import com.choreme.core.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/** Everything the Home tab shows: the weather outside and the day at a glance. */
internal data class HomeUiState(
    val loading: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val weather: HomeWeatherState = HomeWeatherState(),
    val timeline: DayTimeline? = null,
)

/** Reads the family's chores, groceries, calendar, trips and the weather, and refreshes every half hour. */
@HiltViewModel
class HomeViewModel @Inject internal constructor(
    families: FamilyRepository,
    plans: PlanRepository,
    locations: LocationRepository,
    chores: ChoreRepository,
    groceries: GroceryRepository,
    private val calendar: CalendarRepository,
    private val choice: CalendarChoice,
    private val weather: WeatherRepository,
    settings: WeatherSettings,
    private val day: DayWeather,
) : ViewModel() {
    private class Inputs(
        val memberId: String,
        val plans: List<PlanEntry>,
        val places: List<Place>,
        val names: Map<String, String>,
        val units: WeatherUnits,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state: StateFlow<HomeUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            val refresh = flow { while (true) { emit(Unit); delay(30.minutes) } }
            val inputs = combine(
                plans.observe(session.familyId),
                locations.observePlaces(session.familyId),
                families.observeMembers(session.familyId),
                settings.units,
                refresh,
            ) { entries, places, members, units, _ -> Inputs(session.memberId, entries, places, members.associate { it.id to it.name }, units) }
            val events = combine(choice.selected, refresh) { selected, _ -> selected }.flatMapLatest { selected -> eventsToday(selected) }
            combine(inputs, chores.observeChores(session.familyId), groceries.observeItems(session.familyId), events, ::Data)
                .mapLatest { compute(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private class Data(val inputs: Inputs, val chores: List<com.choreme.core.model.chore.Chore>, val groceries: List<GroceryItem>, val events: List<CalendarEvent>)

    private fun eventsToday(selected: Set<Long>?) = if (!calendar.hasPermission() || selected.isNullOrEmpty()) {
        flowOf(emptyList())
    } else {
        val zone = ZoneId.systemDefault()
        val from = LocalDate.now().atStartOfDay(zone).toInstant()
        calendar.observeEvents(selected, from, from.plusSeconds(24 * 3600))
    }

    private suspend fun compute(data: Data): HomeUiState {
        val inputs = data.inputs
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val home = day.home(inputs.places)
        val current = home?.let { weather.forecast(it)?.at(now) }
        val allMoments = home?.let { day.moments(today, inputs.plans, inputs.places, it) }.orEmpty()
        val upcoming = allMoments.filter { it.end.isAfter(now) }
            .map { MomentLine(inputs.names[it.entry.memberId].orEmpty(), it.entry.title, it.entry.start, it.advice) }
        val weatherState = HomeWeatherState(loading = false, now = current, units = inputs.units, moments = upcoming, needsPlace = home == null)
        val board = buildTodayBoard(data.chores, today, inputs.memberId)
        val timeline = dayTimeline(today, data.events, allMoments, inputs.names, board, data.groceries.groceriesToBuy(), ZoneId.systemDefault())
        return HomeUiState(loading = false, date = today, weather = weatherState, timeline = timeline)
    }
}
