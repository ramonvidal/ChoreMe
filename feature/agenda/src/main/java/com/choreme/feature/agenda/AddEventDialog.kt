package com.choreme.feature.agenda

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.agenda.FamilyCalendar
import com.choreme.core.model.agenda.NewEvent
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DURATIONS = listOf(Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2), Duration.ofHours(3))
private val ALERT_MINUTES = listOf(10, 30, 60, 24 * 60)

/** A quick way to add an event; anything fancier (repeating, guests) is done in the calendar app. */
@Composable
internal fun AddEventDialog(
    calendars: List<FamilyCalendar>,
    today: LocalDate,
    onAdd: (calendarId: Long, NewEvent) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var calendarId by remember { mutableStateOf(calendars.first().id) }
    var date by remember { mutableStateOf(today) }
    var allDay by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)) }
    var duration by remember { mutableStateOf(Duration.ofHours(1)) }
    var alert by remember { mutableStateOf(true) }
    var alertMinutes by remember { mutableIntStateOf(30) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    val timeFormat = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_event)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.event_title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (calendars.size > 1) {
                    Text(stringResource(R.string.event_calendar), style = MaterialTheme.typography.labelLarge)
                    calendars.forEach { cal ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(selected = calendarId == cal.id, onClick = { calendarId = cal.id }, role = Role.RadioButton),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = calendarId == cal.id, onClick = null)
                            Text(cal.name)
                        }
                    }
                }

                Text(stringResource(R.string.event_day), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = date == today, onClick = { date = today }, label = { Text(stringResource(R.string.today)) })
                    FilterChip(
                        selected = date == today.plusDays(1),
                        onClick = { date = today.plusDays(1) },
                        label = { Text(stringResource(R.string.tomorrow)) },
                    )
                    val other = date != today && date != today.plusDays(1)
                    FilterChip(
                        selected = other,
                        onClick = { pickingDate = true },
                        label = {
                            Text(if (other) date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) else stringResource(R.string.event_pick_day))
                        },
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.event_all_day), modifier = Modifier.weight(1f))
                    Switch(checked = allDay, onCheckedChange = { allDay = it })
                }
                if (!allDay) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.event_starts, startTime.format(timeFormat)), modifier = Modifier.weight(1f))
                        TextButton(onClick = { pickingTime = true }) { Text(stringResource(R.string.event_change_time)) }
                    }
                    Text(stringResource(R.string.event_duration), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DURATIONS.forEach { option ->
                            FilterChip(selected = duration == option, onClick = { duration = option }, label = { Text(durationLabel(option)) })
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.event_alert), modifier = Modifier.weight(1f))
                    Switch(checked = alert, onCheckedChange = { alert = it })
                }
                if (alert) {
                    if (allDay) {
                        Text(
                            stringResource(R.string.event_alert_evening_before, NewEvent.ALL_DAY_ALERT_TIME.format(timeFormat)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ALERT_MINUTES.forEach { minutes ->
                                FilterChip(
                                    selected = alertMinutes == minutes,
                                    onClick = { alertMinutes = minutes },
                                    label = { Text(alertLabel(minutes)) },
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(
                        calendarId,
                        NewEvent(
                            title = title,
                            date = date,
                            startTime = if (allDay) null else startTime,
                            duration = duration,
                            alert = alert,
                            alertMinutesBefore = alertMinutes,
                        ),
                    )
                },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(CoreUiR.string.add)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )

    if (pickingDate) {
        // The date picker works in UTC milliseconds.
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickingDate = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        ) { DatePicker(state = pickerState) }
    }

    if (pickingTime) {
        val timeState = rememberTimePickerState(initialHour = startTime.hour, initialMinute = startTime.minute, is24Hour = is24Hour)
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    startTime = LocalTime.of(timeState.hour, timeState.minute)
                    pickingTime = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }
}

@Composable
private fun durationLabel(duration: Duration): String =
    if (duration.toMinutes() < 60) {
        stringResource(R.string.duration_minutes, duration.toMinutes().toInt())
    } else {
        val hours = duration.toHours().toInt()
        pluralStringResource(R.plurals.duration_hours, hours, hours)
    }

@Composable
private fun alertLabel(minutes: Int): String = when (minutes) {
    60 -> stringResource(R.string.event_alert_hour)
    24 * 60 -> stringResource(R.string.event_alert_day)
    else -> pluralStringResource(R.plurals.event_alert_minutes, minutes, minutes)
}
