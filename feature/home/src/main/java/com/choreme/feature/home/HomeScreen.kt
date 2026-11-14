package com.choreme.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableIntStateOf
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.ui.SubTab
import com.choreme.core.ui.SubTabs
import com.choreme.core.ui.TabHeader
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.home.DayTimeline
import com.choreme.core.model.home.EventEntry
import com.choreme.core.model.home.TripEntry
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.ui.EmptyState
import com.choreme.core.ui.HeroCard
import com.choreme.core.ui.NosButton
import com.choreme.core.ui.NosButtonStyle
import com.choreme.core.ui.NosCard
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.describe
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class HomeTab { TODAY, AGENDA, WEEK }

/** Today / Agenda / Week under the top edge. */
@Composable
internal fun HomeSubTabs(selected: Int, onSelect: (Int) -> Unit) {
    SubTabs(
        listOf(
            SubTab(stringResource(R.string.home_tab_today), CoreUiR.drawable.ic_tab_today),
            SubTab(stringResource(R.string.home_tab_agenda), CoreUiR.drawable.ic_tab_agenda),
            SubTab(stringResource(R.string.home_tab_week), CoreUiR.drawable.ic_tab_week),
        ),
        selected,
        onSelect,
    )
}

/** Casa: the sub-tabs Today (the weather house and the day), Agenda (the family calendar) and Week (the regular trips). */
@Composable
internal fun HomeScreen(
    onOpenChores: () -> Unit,
    onAddChore: () -> Unit,
    onOpenGroceries: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(HomeTab.TODAY) }
    // Counts the "New event" requests, so the Agenda sub-tab opens its dialog once per request.
    var newEvent by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        HomeSubTabs(tab.ordinal) { tab = HomeTab.entries[it] }
        when (tab) {
            HomeTab.TODAY -> HomeContent(
                state,
                onOpenChores = onOpenChores,
                onAddChore = onAddChore,
                onOpenGroceries = onOpenGroceries,
                onAddEvent = { newEvent++; tab = HomeTab.AGENDA },
            )
            HomeTab.AGENDA -> AgendaScreen(addRequest = newEvent)
            HomeTab.WEEK -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                TabHeader(stringResource(R.string.plan_title))
                Box(Modifier.padding(top = 8.dp)) { WeeklyPlan() }
            }
        }
    }
}

/** The Home tab, drawn from [state] alone so it can be screenshotted: the weather house, the day's card and its timeline. */
@Composable
internal fun HomeContent(
    state: HomeUiState,
    onOpenChores: () -> Unit,
    onAddChore: () -> Unit,
    onOpenGroceries: () -> Unit,
    onAddEvent: () -> Unit,
) {
    var showWeather by rememberSaveable { mutableStateOf(false) }
    var addMenu by remember { mutableStateOf(false) }
    val timeline = state.timeline
    val date = remember(state.date) {
        state.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).replaceFirstChar { it.uppercase() }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The little house with the real weather, first.
        item(key = "weather") { HomeWeatherCard(state.weather, onClick = { showWeather = true }) }

        if (timeline != null) {
            item(key = "day") {
                HeroCard(
                    title = stringResource(R.string.home_today),
                    kicker = date,
                    line = summaryLine(timeline),
                ) {
                    // The one filled button: the three things done every day.
                    Box {
                        NosButton(stringResource(R.string.home_add), { addMenu = true }, Modifier.fillMaxWidth())
                        DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_chore)) }, onClick = { addMenu = false; onAddChore() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_grocery)) }, onClick = { addMenu = false; onOpenGroceries() })
                            DropdownMenuItem(text = { Text(stringResource(R.string.home_add_event)) }, onClick = { addMenu = false; onAddEvent() })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NosButton(
                            stringResource(R.string.home_open_chores, timeline.choreCount),
                            onOpenChores,
                            Modifier.weight(1f),
                            style = NosButtonStyle.Tonal,
                        )
                        NosButton(
                            stringResource(R.string.home_open_groceries, timeline.groceriesToBuy),
                            onOpenGroceries,
                            Modifier.weight(1f),
                            style = NosButtonStyle.Tonal,
                        )
                    }
                }
            }
            item(key = "timeline-title") { SectionLabel(stringResource(R.string.home_timeline)) }
            item(key = "timeline") {
                if (timeline.isEmpty) {
                    EmptyState(title = stringResource(R.string.home_nothing_title), body = stringResource(R.string.home_nothing_body))
                } else {
                    TimelineCard(timeline, state.weather.units)
                }
            }
        }
    }

    if (showWeather) HomeWeatherDialog(state.weather, onDismiss = { showWeather = false })
}

@Composable
private fun summaryLine(timeline: DayTimeline): String = listOf(
    pluralStringResource(R.plurals.home_chores, timeline.choreCount, timeline.choreCount),
    pluralStringResource(R.plurals.home_events, timeline.eventCount, timeline.eventCount),
    pluralStringResource(R.plurals.home_groceries, timeline.groceriesToBuy, timeline.groceriesToBuy),
).joinToString(" · ")

/** All-day things first, then the events and trips in the order of the day. */
@Composable
private fun TimelineCard(timeline: DayTimeline, units: WeatherUnits) {
    val context = LocalContext.current
    val time = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }
    NosCard(Modifier.fillMaxWidth()) {
        var first = true
        timeline.allDay.forEach { title ->
            if (!first) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            first = false
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.home_all_day), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        timeline.entries.forEach { entry ->
            if (!first) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            first = false
            when (entry) {
                is EventEntry -> {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        listOfNotNull(entry.time.format(time), entry.place).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is TripEntry -> {
                    Text(
                        "${entry.moment.entry.title}${if (entry.who.isNotEmpty()) " · ${entry.who}" else ""}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    val advice = entry.moment.advice.describe(context, units).ifEmpty { stringResource(R.string.weather_all_good) }
                    Text(
                        "${entry.time.format(time)} · $advice",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (entry.moment.advice.isQuiet) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
