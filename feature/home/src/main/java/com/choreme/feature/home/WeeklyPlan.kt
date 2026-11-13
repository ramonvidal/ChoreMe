package com.choreme.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.location.Place
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.TripMode
import com.choreme.core.ui.NosButton
import com.choreme.core.ui.NosButtonStyle
import com.choreme.core.ui.NosChip
import com.choreme.core.ui.NosCard
import com.choreme.core.ui.R as CoreUiR
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

internal data class WeeklyPlanState(
    val entries: List<PlanEntry> = emptyList(),
    val places: List<Place> = emptyList(),
    val members: List<Member> = emptyList(),
    val isParent: Boolean = false,
    val myMemberId: String = "",
) {
    fun nameOf(memberId: String) = members.firstOrNull { it.id == memberId }?.name.orEmpty()
}

/** The family's regular trips for the weather. Everyone sees them; parents add and change them. */
@HiltViewModel
class WeeklyPlanViewModel @Inject constructor(
    families: FamilyRepository,
    private val plans: PlanRepository,
    locations: LocationRepository,
) : ViewModel() {
    private val session = families.session.filterIsInstance<SessionState.InFamily>()
    private var familyId: String? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val state: StateFlow<WeeklyPlanState> = session.flatMapLatest { s ->
        familyId = s.familyId
        combine(plans.observe(s.familyId), locations.observePlaces(s.familyId), families.observeMembers(s.familyId)) { entries, places, members ->
            WeeklyPlanState(
                entries = entries.sortedWith(compareBy({ it.memberId }, { it.start }, { it.title.lowercase() })),
                places = places.sortedBy { it.name.lowercase() },
                members = members,
                isParent = s.role == Role.PARENT,
                myMemberId = s.memberId,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeeklyPlanState())

    internal fun save(entry: PlanEntry) {
        familyId?.let { plans.save(it, entry) }
    }

    internal fun delete(entry: PlanEntry) {
        familyId?.let { plans.delete(it, entry.id) }
    }
}

/** "Weekly plan" on the Family tab: the regular trips the weather is told for, and a button to add one. */
@Composable
internal fun WeeklyPlan(viewModel: WeeklyPlanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WeeklyPlanContent(state, onSave = viewModel::save, onDelete = viewModel::delete)
}

@Composable
internal fun WeeklyPlanContent(state: WeeklyPlanState, onSave: (PlanEntry) -> Unit, onDelete: (PlanEntry) -> Unit) {
    var editing by remember { mutableStateOf<PlanEntry?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val time = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.plan_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        NosCard(Modifier.fillMaxWidth()) {
            if (state.entries.isEmpty()) {
                Text(stringResource(R.string.plan_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                Column(Modifier.fillMaxWidth().then(if (state.isParent) Modifier.clickable { editing = entry } else Modifier)) {
                    Text("${entry.title} · ${state.nameOf(entry.memberId)}", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${daysSummary(entry.days)} · ${entry.start.format(time)} · ${stringResource(entry.mode.label)} · ${stringResource(R.string.plan_minutes, entry.minutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.isParent) {
            if (state.places.isEmpty()) {
                Text(stringResource(R.string.plan_no_places), style = MaterialTheme.typography.bodySmall)
            } else {
                NosButton(stringResource(R.string.plan_add), onClick = { adding = true }, modifier = Modifier.fillMaxWidth(), style = NosButtonStyle.Tonal)
            }
        }
    }

    if (adding) {
        TripDialog(state, entry = null, onDismiss = { adding = false }, onSave = { onSave(it); adding = false })
    }
    editing?.let { entry ->
        TripDialog(
            state, entry = entry,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(entry); editing = null },
        )
    }
}

private val WEEKDAYS = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

@Composable
private fun daysSummary(days: Set<DayOfWeek>): String = when (days) {
    WEEKDAYS -> stringResource(R.string.plan_weekdays)
    WEEKEND -> stringResource(R.string.plan_weekend)
    DayOfWeek.entries.toSet() -> stringResource(R.string.plan_every_day)
    else -> days.sorted().joinToString(" ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

private val TripMode.label: Int
    get() = when (this) {
        TripMode.WALK -> R.string.mode_walk
        TripMode.BIKE -> R.string.mode_bike
        TripMode.CAR -> R.string.mode_car
        TripMode.TRANSIT -> R.string.mode_transit
        TripMode.OTHER -> R.string.mode_other
    }

/** Adds or changes one trip: where to (a saved place), who, which days, when, how long and how. */
@Composable
private fun TripDialog(
    state: WeeklyPlanState,
    entry: PlanEntry?,
    onDismiss: () -> Unit,
    onSave: (PlanEntry) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var placeId by rememberSaveable { mutableStateOf(entry?.placeId ?: state.places.firstOrNull()?.id) }
    var memberId by rememberSaveable { mutableStateOf(entry?.memberId ?: state.myMemberId) }
    var days by remember { mutableStateOf(entry?.days ?: WEEKDAYS) }
    var start by remember { mutableStateOf(entry?.start ?: LocalTime.of(8, 0)) }
    var minutes by rememberSaveable { mutableStateOf(entry?.minutes ?: PlanEntry.DEFAULT_MINUTES) }
    var mode by rememberSaveable { mutableStateOf(entry?.mode ?: TripMode.OTHER) }
    var pickingTime by remember { mutableStateOf(false) }
    val time = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (entry == null) R.string.plan_add else R.string.plan_edit)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.plan_place), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.places.forEach { place -> NosChip(place.name, placeId == place.id, { placeId = place.id }) }
                }
                if (state.members.size > 1) {
                    Text(stringResource(R.string.plan_who), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.members.sortedBy { it.name.lowercase() }.forEach { m -> NosChip(m.name, memberId == m.id, { memberId = m.id }) }
                    }
                }
                Text(stringResource(R.string.plan_days), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NosChip(stringResource(R.string.plan_weekdays), days == WEEKDAYS, { days = WEEKDAYS })
                    NosChip(stringResource(R.string.plan_weekend), days == WEEKEND, { days = WEEKEND })
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        NosChip(
                            day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            day in days,
                            { days = if (day in days) days - day else days + day },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NosButton(stringResource(R.string.plan_time, start.format(time)), onClick = { pickingTime = true }, style = NosButtonStyle.Tonal)
                }
                Text(stringResource(R.string.plan_duration), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanEntry.MINUTE_CHOICES.forEach { m -> NosChip(stringResource(R.string.plan_minutes, m), minutes == m, { minutes = m }) }
                }
                Text(stringResource(R.string.plan_mode), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripMode.entries.forEach { m -> NosChip(stringResource(m.label), mode == m, { mode = m }) }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.plan_delete), color = MaterialTheme.colorScheme.error) }
                }
            }
        },
        confirmButton = {
            val place = state.places.firstOrNull { it.id == placeId }
            TextButton(
                enabled = place != null && days.isNotEmpty() && memberId.isNotEmpty(),
                onClick = {
                    if (place != null) {
                        onSave(
                            PlanEntry(
                                id = entry?.id.orEmpty(),
                                memberId = memberId,
                                title = place.name.take(PlanEntry.MAX_TITLE),
                                days = days,
                                start = start,
                                minutes = minutes,
                                mode = mode,
                                placeId = place.id,
                            ),
                        )
                    }
                },
            ) { Text(stringResource(CoreUiR.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )

    if (pickingTime) {
        val picker = rememberTimePickerState(start.hour, start.minute, android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { TimePicker(state = picker) },
            confirmButton = {
                TextButton(onClick = { start = LocalTime.of(picker.hour, picker.minute); pickingTime = false }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }
}
