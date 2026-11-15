package com.choreme.feature.home

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
import com.choreme.core.model.home.Occasion
import com.choreme.core.model.home.SceneInput
import com.choreme.core.model.weather.Advice
import com.choreme.core.model.weather.SunTimes
import com.choreme.core.model.weather.AirNote
import com.choreme.core.model.weather.DryingTip
import com.choreme.core.model.weather.Sky
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.ui.degrees
import com.choreme.core.ui.describe
import com.choreme.core.ui.label
import com.choreme.core.ui.windText
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One moment of today for the card: who, where (the saved place's name), when, and what to know about the weather. */
internal data class MomentLine(val who: String, val title: String, val time: LocalTime, val advice: Advice)

/** What the little house card says: the weather outside now and the rest of today's regular trips. */
internal data class HomeWeatherState(
    val loading: Boolean = true,
    /** Null until a forecast arrives (or when there is no place to ask for). */
    val now: WeatherHour? = null,
    val units: WeatherUnits = WeatherUnits(),
    val moments: List<MomentLine> = emptyList(),
    /** The air and pollen for the next hours; says nothing on an ordinary day. */
    val air: AirNote = AirNote(),
    /** What to do with the washing, only while laundry is waiting. */
    val drying: DryingTip? = null,
    /** No position and no saved place: the family has to save its home on the Map tab. */
    val needsPlace: Boolean = false,
    /** The hour for the picture; null means the real one (tests set it to get a steady picture). */
    val hour: Float? = null,
    /** How many family members are at home now: their windows are lit. */
    val peopleHome: Int = 0,
    /** An urgent chore is waiting: the mailbox flag is up. */
    val urgent: Boolean = false,
    /** The special date the house is dressed for, if any. */
    val occasion: Occasion? = null,
    /** Whether the picture moves (the phone's own choice). */
    val animated: Boolean = true,
    /** The sun where the house is, so night falls when the sun really sets; null until the place is known. */
    val sun: SunTimes? = null,
) {
    fun scene(hour: Float) = SceneInput(
        hour = hour,
        sky = now?.sky ?: Sky.CLEAR,
        windKmh = (now?.windKmh ?: 5.0).toFloat(),
        peopleHome = peopleHome,
        urgent = urgent,
        occasion = occasion,
        sunrise = (sun?.sunrise ?: 6f).coerceIn(3f, 10f),
        sunset = (sun?.sunset ?: 18.5f).coerceIn(15f, 21.5f),
    )
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
            LiveHouse(state.scene(state.hour ?: rememberHour()), Modifier.fillMaxWidth(), animated = state.animated)
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
                    state.drying?.let { Text(dryingText(it), style = MaterialTheme.typography.bodyMedium) }
                    state.air.lines().forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
                    // The trips and their weather are listed once, under "The day"; the card is the weather outside.
                }
            }
        }
    }
}

@Composable
private fun dryingText(tip: DryingTip): String = when (tip) {
    DryingTip.HangToday -> stringResource(R.string.drying_today)
    is DryingTip.WaitFor -> stringResource(
        R.string.drying_wait,
        tip.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()),
    )
    DryingTip.Indoors -> stringResource(R.string.drying_indoors)
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
                LiveHouse(state.scene(state.hour ?: rememberHour()), Modifier.fillMaxWidth(), animated = state.animated)
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
                state.drying?.let { Text(dryingText(it), style = MaterialTheme.typography.bodyMedium) }
                state.air.lines().forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
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
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.weather_close)) } },
    )
}
