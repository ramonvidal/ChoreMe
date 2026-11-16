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
import com.choreme.core.model.chore.RoomFilter
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.chore.filteredBy
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.groceriesToBuy
import com.choreme.core.model.home.DayTimeline
import com.choreme.core.model.home.dayTimeline
import com.choreme.core.model.location.Place
import com.choreme.core.model.session.SessionState
import com.choreme.core.location.RoutineLog
import com.choreme.core.location.SharingChoice
import com.choreme.core.model.Role
import com.choreme.core.model.home.SetupFacts
import com.choreme.core.settings.HintSettings
import com.choreme.core.model.home.occasionOn
import com.choreme.core.model.home.peopleAtHome
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.weather.homePlace
import com.choreme.core.model.weather.sunTimes
import com.choreme.core.settings.AppearanceSettings
import java.time.Instant
import kotlinx.coroutines.flow.onStart
import com.choreme.core.model.weather.AirNote
import com.choreme.core.model.weather.activeEntries
import com.choreme.core.model.weather.DRYING_LOOKAHEAD
import com.choreme.core.model.weather.dayOutlook
import com.choreme.core.model.weather.dryingTip
import com.choreme.core.model.weather.laundryDue
import com.choreme.core.model.weather.airNote
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
    /** What is set up so far, for the getting-started card; null until the family is known. */
    val setup: SetupFacts? = null,
    /** The person hid the getting-started card for good. */
    val setupHidden: Boolean = false,
)

/** Reads the family's chores, groceries, calendar, trips and the weather, and refreshes every ten minutes. */
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
    appearance: AppearanceSettings,
    private val day: DayWeather,
    private val routine: RoutineLog,
    sharing: SharingChoice,
    hints: HintSettings,
) : ViewModel() {
    private val sharingNow = sharing.isSharing
    private val setupHidden = hints.firstRunHidden
    private val hintSettings = hints

    private val animatedHouse = appearance.animatedHouse

    private class Inputs(
        val memberId: String,
        val plans: List<PlanEntry>,
        val places: List<Place>,
        val names: Map<String, String>,
        val units: WeatherUnits,
        val hasAnyTrip: Boolean,
        val isParent: Boolean,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state: StateFlow<HomeUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            val refresh = flow { while (true) { emit(Unit); delay(10.minutes) } }
            val inputs = combine(
                plans.observe(session.familyId),
                locations.observePlaces(session.familyId),
                families.observeMembers(session.familyId),
                settings.units,
                refresh,
            ) { entries, places, members, units, _ ->
                val active = activeEntries(entries, members, LocalDate.now(), session.memberId, routine.lastVisits())
                Inputs(session.memberId, active, places, members.associate { it.id to it.name }, units, entries.isNotEmpty(), session.role == Role.PARENT) }
            val events = combine(choice.selected, refresh) { selected, _ -> selected }.flatMapLatest { selected -> eventsToday(selected) }
            // Who is at home now (for the lit windows) and whether the little house moves.
            val house = combine(locations.observeLocations(session.familyId).onStart { emit(emptyList()) }, animatedHouse, sharingNow, setupHidden) { l, a, sh, hid ->
                House(l, a, sh, hid)
            }
            combine(inputs, chores.observeChores(session.familyId), groceries.observeItems(session.familyId), events, house) { i, c, g, e, h ->
                Data(i, c, g, e, h.locations, h.animated, h.sharing, h.setupHidden)
            }.mapLatest { compute(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private class Data(val inputs: Inputs, val chores: List<com.choreme.core.model.chore.Chore>, val groceries: List<GroceryItem>, val events: List<CalendarEvent>, val locations: List<MemberLocation>, val animated: Boolean, val sharing: Boolean, val setupHidden: Boolean)

    private class House(val locations: List<MemberLocation>, val animated: Boolean, val sharing: Boolean, val setupHidden: Boolean)

    fun hideSetup() = hintSettings.hideFirstRun()

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
        val air = home?.let { airNote(weather.air(it)?.between(now, now.plusHours(12)).orEmpty()) } ?: AirNote()
        val outlook = home?.let { weather.forecast(it) }?.let { forecast ->
            (0..DRYING_LOOKAHEAD).mapNotNull { dayOutlook(today.plusDays(it.toLong()), forecast.hours, emptyList()) }
        }.orEmpty()
        val drying = dryingTip(laundryDue(data.chores, today), outlook)
        // The house's own chores and the tasks of the rooms are counted apart, as the Chores tab keeps them apart.
        val board = buildTodayBoard(data.chores.filteredBy(RoomFilter.NO_ROOM), today, inputs.memberId)
        val roomBoard = buildTodayBoard(data.chores.filteredBy(RoomFilter.ROOMS), today, inputs.memberId)
        val timeline = dayTimeline(today, data.events, allMoments, inputs.names, board, data.groceries.groceriesToBuy(), ZoneId.systemDefault(), roomBoard, now)
        val weatherState = HomeWeatherState(
            loading = false, now = current, units = inputs.units, moments = upcoming, air = air, drying = drying, needsPlace = home == null,
            peopleHome = peopleAtHome(data.locations, homePlace(inputs.places), Instant.now()),
            urgent = timeline.urgent.isNotEmpty(),
            occasion = occasionOn(today),
            animated = data.animated,
            sun = home?.let { sunTimes(today, it.latitude, it.longitude, ZoneId.systemDefault().rules.getOffset(Instant.now()).totalSeconds / 3600.0) },
        )
        val setup = SetupFacts(
            hasPlace = inputs.places.isNotEmpty(), sharing = data.sharing, notificationsAllowed = true,
            hasTrip = inputs.hasAnyTrip, isParent = inputs.isParent,
        )
        return HomeUiState(loading = false, date = today, weather = weatherState, timeline = timeline, setup = setup, setupHidden = data.setupHidden)
    }
}
