package com.choreme.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.LocationRepository
import com.choreme.core.data.PlanRepository
import com.choreme.core.location.RoutineLog
import com.choreme.core.model.Member
import com.choreme.core.model.location.Place
import com.choreme.core.model.weather.DayOutlook
import com.choreme.core.model.weather.DryingQuality
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.model.weather.activeEntries
import com.choreme.core.model.weather.dayOutlook
import com.choreme.core.settings.WeatherSettings
import com.choreme.core.ui.NosButton
import com.choreme.core.ui.NosButtonStyle
import com.choreme.core.ui.NosCard
import com.choreme.core.ui.TabHeader
import com.choreme.core.ui.degrees
import com.choreme.core.ui.describe
import com.choreme.core.ui.label
import com.choreme.core.weather.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/** What the Week sub-tab shows: the next days and the family's trips in them (the trips are planned on the Map tab). */
internal data class WeekAheadState(
    val outlook: List<DayOutlook> = emptyList(),
    val units: WeatherUnits = WeatherUnits(),
    val members: List<Member> = emptyList(),
    val myMemberId: String = "",
    val hasTrips: Boolean = false,
    val loading: Boolean = true,
) {
    fun nameOf(memberId: String) = members.firstOrNull { it.id == memberId }?.name.orEmpty()
}

/** The forecast of the week ahead with each trip of the plan (paused ones left out) and its weather. */
@HiltViewModel
class WeekAheadViewModel @Inject internal constructor(
    families: FamilyRepository,
    plans: PlanRepository,
    locations: LocationRepository,
    private val weather: WeatherRepository,
    private val day: DayWeather,
    settings: WeatherSettings,
    private val routine: RoutineLog,
) : ViewModel() {
    private class Inputs(val memberId: String, val entries: List<com.choreme.core.model.weather.PlanEntry>, val places: List<Place>, val members: List<Member>)

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state: StateFlow<WeekAheadState> = families.inFamily.flatMapLatest { s ->
        combine(plans.observe(s.familyId), locations.observePlaces(s.familyId), families.observeMembers(s.familyId), settings.units) { entries, places, members, units ->
            Pair(Inputs(s.memberId, entries, places, members), units)
        }
    }.mapLatest { (inputs, units) ->
        val today = LocalDate.now()
        val active = activeEntries(inputs.entries, inputs.members, today, inputs.memberId, routine.lastVisits())
        val home = day.home(inputs.places)
        val forecast = home?.let { weather.forecast(it) }
        val outlook = if (home == null || forecast == null) emptyList() else (0 until OUTLOOK_DAYS).mapNotNull { offset ->
            val date = today.plusDays(offset.toLong())
            dayOutlook(date, forecast.hours, day.moments(date, active, inputs.places, home))
        }
        WeekAheadState(outlook, units, inputs.members, inputs.memberId, hasTrips = inputs.entries.isNotEmpty(), loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeekAheadState())
}

private const val OUTLOOK_DAYS = 7
private val DAY_LABEL_WIDTH = 64.dp

/** The Week sub-tab: the week ahead, and a way to the Map tab where the trips are planned. */
@Composable
internal fun WeekAheadScreen(onPlanTrips: () -> Unit, viewModel: WeekAheadViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeekAheadContent(state, onPlanTrips)
}

@Composable
internal fun WeekAheadContent(state: WeekAheadState, onPlanTrips: () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TabHeader(stringResource(R.string.week_title))
        if (state.outlook.isEmpty() && !state.loading) {
            Text(stringResource(R.string.week_unavailable), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.outlook.isNotEmpty()) WeekAhead(state)
        Text(
            stringResource(if (state.hasTrips) R.string.week_trips_hint else R.string.week_no_trips),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        NosButton(stringResource(R.string.week_plan_trips), onClick = onPlanTrips, modifier = Modifier.fillMaxWidth(), style = NosButtonStyle.Tonal)
    }
}

/** The next days: the sky, the warmth and the chance of rain, with each trip of that day and its weather under it. */
@Composable
private fun WeekAhead(state: WeekAheadState) {
    val context = LocalContext.current
    val time = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    NosCard(Modifier.fillMaxWidth()) {
        state.outlook.forEachIndexed { index, day ->
            if (index > 0) HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            // The day keeps a fixed width so a long description never squeezes it into one letter per line.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "${day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${day.date.dayOfMonth}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.width(DAY_LABEL_WIDTH),
                )
                Text(
                    listOf(
                        stringResource(day.sky.label),
                        "${state.units.degrees(day.lowC)} / ${state.units.degrees(day.highC)}",
                        if (day.rainChance >= 30) stringResource(R.string.week_rain, day.rainChance) else null,
                        if (day.drying == DryingQuality.GOOD) stringResource(R.string.week_drying) else null,
                    ).filterNotNull().joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            day.moments.forEach { moment ->
                val who = if (moment.entry.memberId == state.myMemberId) "" else " (${state.nameOf(moment.entry.memberId)})"
                val advice = moment.advice.describe(context, state.units).ifEmpty { stringResource(R.string.weather_all_good) }
                Text(
                    "${moment.entry.start.format(time)} ${moment.entry.title}$who: $advice",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (moment.advice.isQuiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

