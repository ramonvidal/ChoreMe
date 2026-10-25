package com.choreme.feature.agenda

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.calendar.CalendarRepository
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.agenda.FamilyCalendar
import com.choreme.core.model.chore.Chore
import com.choreme.core.ui.ErrorText
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.icon
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
internal fun AgendaScreen(viewModel: AgendaViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var choosing by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.onPermissionResult()
    }

    fun open(event: CalendarEvent) {
        try {
            context.startActivity(viewModel.openIntent(event))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
        }
    }

    if (state.needsChoice || choosing) {
        CalendarPicker(
            calendars = state.calendars,
            initial = state.selected ?: state.suggestion,
            onSave = {
                viewModel.choose(it)
                choosing = false
            },
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        ) {
            FilterChip(
                selected = state.view == AgendaView.LIST,
                onClick = { viewModel.showView(AgendaView.LIST) },
                label = { Text(stringResource(R.string.view_list)) },
            )
            FilterChip(
                selected = state.view == AgendaView.MONTH,
                onClick = { viewModel.showView(AgendaView.MONTH) },
                label = { Text(stringResource(R.string.view_month)) },
            )
        }
        if (state.view == AgendaView.MONTH && state.hasPermission) {
            MonthView(
                state = state,
                onMonth = viewModel::showMonth,
                onSelect = viewModel::selectDay,
                onOpenEvent = ::open,
                modifier = Modifier.fillMaxSize(),
            )
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        ) {
            when {
                !state.hasPermission -> item(key = "permission") {
                    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.permission_body))
                            Button(onClick = { permissionLauncher.launch(CalendarRepository.PERMISSIONS) }) {
                                Text(stringResource(R.string.permission_allow))
                            }
                        }
                    }
                }
                state.calendars.isEmpty() -> item(key = "no-calendars") {
                    Text(stringResource(R.string.no_calendars), modifier = Modifier.padding(vertical = 8.dp))
                }
                else -> item(key = "calendars") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            state.selected.orEmpty().mapNotNull(state::calendarName).joinToString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        TextButton(onClick = { choosing = true }) { Text(stringResource(R.string.change_calendars)) }
                    }
                }
            }

            state.days.forEach { day ->
                item(key = "day-${day.date}") { DayHeader(day.date, state.today) }
                if (day.events.isEmpty() && day.chores.isEmpty()) {
                    item(key = "empty-${day.date}") {
                        Text(
                            stringResource(R.string.nothing_today),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
                items(day.events, key = { "event-${day.date}-${it.id}-${it.start}" }) { event ->
                    EventRow(event, onClick = { open(event) })
                }
                items(day.chores, key = { "chore-${day.date}-${it.id}" }) { chore -> ChoreRow(chore) }
            }
            item(key = "error") { ErrorText(actionState.error) }
        }
        }

        if (state.writableSelected.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_event)) },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            )
        }
    }

    if (adding) {
        AddEventDialog(
            calendars = state.writableSelected,
            today = state.today,
            initialDate = if (state.view == AgendaView.MONTH) maxOf(state.selectedDay, state.today) else state.today,
            onAdd = { calendarId, event ->
                viewModel.addEvent(calendarId, event)
                adding = false
            },
            onDismiss = { adding = false },
        )
    }
}

@Composable
private fun DayHeader(date: LocalDate, today: LocalDate) {
    val label = when (date) {
        today -> stringResource(R.string.today)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())).replaceFirstChar { it.uppercase() }
    }
    Column(Modifier.padding(top = 16.dp, bottom = 4.dp)) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun EventRow(event: CalendarEvent, onClick: () -> Unit) {
    val zone = ZoneId.systemDefault()
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(12.dp).background(Color(event.color), CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(
            if (event.allDay) {
                stringResource(R.string.all_day)
            } else {
                stringResource(R.string.time_range, event.start.atZone(zone).format(time), event.end.atZone(zone).format(time))
            },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(112.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            event.location?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun ChoreRow(chore: Chore) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        ItemIcon(chore.template.icon, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(R.string.chore_due),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(chore.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CalendarPicker(calendars: List<FamilyCalendar>, initial: Set<Long>, onSave: (Set<Long>) -> Unit) {
    var selected by remember(initial) { mutableStateOf(initial) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Text(stringResource(R.string.choose_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.choose_hint), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))
        }
        calendars.groupBy { it.account }.forEach { (account, inAccount) ->
            item(key = "account-$account") {
                Text(account, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
            }
            items(inAccount, key = { it.id }) { cal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = cal.id in selected,
                            onValueChange = { on -> selected = if (on) selected + cal.id else selected - cal.id },
                            role = Role.Checkbox,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = cal.id in selected, onCheckedChange = null)
                    Box(Modifier.size(12.dp).background(Color(cal.color), CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(cal.name, modifier = Modifier.weight(1f))
                    if (!cal.isWritable) {
                        Text(stringResource(R.string.read_only), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Button(onClick = { onSave(selected) }, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.choose_save))
            }
        }
    }
}
