package com.choreme.feature.chores

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.LocationRepository
import com.choreme.core.data.PlanRepository
import com.choreme.core.location.DeviceLocation
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.weather.Advice
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.Sky
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.model.weather.homePlace
import com.choreme.core.model.weather.momentsOn
import com.choreme.core.settings.WeatherSettings
import com.choreme.core.ui.degrees
import com.choreme.core.ui.describe
import com.choreme.core.ui.label
import com.choreme.core.ui.windText
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
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/** One moment of today for the card: who, where (the saved place's name), when, and what to know about the weather. */
internal data class MomentLine(val who: String, val title: String, val time: LocalTime, val advice: Advice)

/** What the little house card says: the weather outside now and the rest of today's regular trips. */
internal data class HomeWeatherState(
    val loading: Boolean = true,
    /** Null until a forecast arrives (or when there is no place to ask for). */
    val now: WeatherHour? = null,
    val units: WeatherUnits = WeatherUnits(),
    val moments: List<MomentLine> = emptyList(),
    /** No position and no saved place: the family has to save its home on the Map tab. */
    val needsPlace: Boolean = false,
) {
    val look: WeatherLook get() = WeatherLook(now?.sky ?: Sky.CLEAR, now?.isDay ?: true)
}

/** Fetches the weather for the card every half hour while the tab is open. */
@HiltViewModel
class HomeWeatherViewModel @Inject constructor(
    families: FamilyRepository,
    plans: PlanRepository,
    locations: LocationRepository,
    private val weather: WeatherRepository,
    settings: WeatherSettings,
    private val device: DeviceLocation,
) : ViewModel() {
    private class Inputs(
        val plans: List<PlanEntry>,
        val places: List<com.choreme.core.model.location.Place>,
        val names: Map<String, String>,
        val units: WeatherUnits,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state: StateFlow<HomeWeatherState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            val refresh = flow { while (true) { emit(Unit); delay(30.minutes) } }
            combine(
                plans.observe(session.familyId),
                locations.observePlaces(session.familyId),
                families.observeMembers(session.familyId),
                settings.units,
                refresh,
            ) { entries, places, members, units, _ -> Inputs(entries, places, members.associate { it.id to it.name }, units) }
                .mapLatest { compute(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeWeatherState())

    private suspend fun compute(inputs: Inputs): HomeWeatherState {
        // Where it's raining on us: the phone's own (approximate) position, else the family's home on the map.
        val home = device.current()?.point ?: homePlace(inputs.places)?.center
            ?: return HomeWeatherState(loading = false, units = inputs.units, needsPlace = true)
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val current = weather.forecast(home)?.at(now)
        val hoursByEntry = inputs.plans
            .filter { it.isOn(today.dayOfWeek) }
            .associate { entry ->
                val from = today.atTime(entry.start)
                val points = listOfNotNull(home, inputs.places.firstOrNull { it.id == entry.placeId }?.center)
                entry.id to weather.hoursFor(points, from, from.plusMinutes(entry.minutes.toLong()))
            }
        val moments = momentsOn(today, inputs.plans) { entry, _, _ -> hoursByEntry[entry.id].orEmpty() }
            .filter { it.end.isAfter(now) }
            .map { MomentLine(inputs.names[it.entry.memberId].orEmpty(), it.entry.title, it.entry.start, it.advice) }
        return HomeWeatherState(loading = false, now = current, units = inputs.units, moments = moments)
    }
}

/** The little house with the real weather, the temperature and the next trips. Tap for the day. */
@Composable
internal fun HomeWeatherCard(state: HomeWeatherState, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Box {
            HomePicture(state.look, Modifier.fillMaxWidth().height(124.dp))
            state.now?.let { now ->
                Text(
                    state.units.degrees(now.temperatureC),
                    style = MaterialTheme.typography.headlineSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f), MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val now = state.now
            when {
                state.needsPlace -> Text(stringResource(R.string.weather_needs_place), style = MaterialTheme.typography.bodyMedium)
                now == null -> Text(
                    stringResource(if (state.loading) R.string.weather_loading else R.string.weather_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    Text(
                        listOfNotNull(stringResource(now.sky.label), now.feelsLikeC?.let { stringResource(R.string.weather_feels_like, state.units.degrees(it)) })
                            .joinToString(" · "),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    state.moments.take(2).forEach { MomentText(it, state.units, context) }
                    if (state.moments.isEmpty()) {
                        Text(
                            stringResource(R.string.weather_nothing_planned),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentText(line: MomentLine, units: WeatherUnits, context: android.content.Context) {
    val time = remember(line.time) { line.time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) }
    val advice = line.advice.describe(context, units).ifEmpty { stringResource(R.string.weather_all_good) }
    Text(
        "$time · ${line.title}: $advice",
        style = MaterialTheme.typography.bodyMedium,
        color = if (line.advice.isQuiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}

/** The rest of today, trip by trip, and what it is like outside right now. */
@Composable
internal fun HomeWeatherDialog(state: HomeWeatherState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weather_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HomePicture(state.look, Modifier.fillMaxWidth().height(150.dp))
                state.now?.let { now ->
                    Text(
                        listOf(
                            stringResource(now.sky.label),
                            state.units.degrees(now.temperatureC),
                            stringResource(R.string.weather_wind, state.units.windText(now.windKmh)),
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (state.moments.isEmpty()) {
                    Text(stringResource(R.string.weather_nothing_planned), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column {
                        state.moments.forEachIndexed { index, line ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                            Row(Modifier.padding(vertical = 8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("${line.title}${if (line.who.isNotEmpty()) " · ${line.who}" else ""}", style = MaterialTheme.typography.titleSmall)
                                    MomentText(line, state.units, context)
                                }
                            }
                        }
                    }
                }
                Text(stringResource(R.string.weather_credit), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.home_close)) } },
    )
}
