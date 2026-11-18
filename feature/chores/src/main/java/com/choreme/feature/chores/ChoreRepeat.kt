package com.choreme.feature.chores

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.choreme.core.model.chore.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset
import java.time.Instant
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker

internal enum class RepeatKind { ONCE, EVERY_DAYS, WEEKLY, MONTHLY }

@Composable
internal fun RepeatSection(
    kind: RepeatKind,
    everyDays: Int,
    weekDays: Set<DayOfWeek>,
    monthDay: Int,
    dueDate: LocalDate,
    firstDue: LocalDate,
    repeatUntil: LocalDate?,
    today: LocalDate,
    onKind: (RepeatKind) -> Unit,
    onEveryDays: (Int) -> Unit,
    onWeekDays: (Set<DayOfWeek>) -> Unit,
    onMonthDay: (Int) -> Unit,
    onDueDate: (LocalDate) -> Unit,
    onRepeatUntil: (LocalDate?) -> Unit,
) {
    var pickingEnd by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        RepeatOption(stringResource(R.string.repeat_once), kind == RepeatKind.ONCE) { onKind(RepeatKind.ONCE) }
        RepeatOption(stringResource(R.string.repeat_daily), kind == RepeatKind.EVERY_DAYS && everyDays == 1) {
            onKind(RepeatKind.EVERY_DAYS)
            onEveryDays(1)
        }
        RepeatOption(stringResource(R.string.editor_every_n_days), kind == RepeatKind.EVERY_DAYS && everyDays > 1) {
            onKind(RepeatKind.EVERY_DAYS)
            if (everyDays == 1) onEveryDays(2)
        }
        if (kind == RepeatKind.EVERY_DAYS && everyDays > 1) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                OutlinedIconButton(onClick = { if (everyDays > 2) onEveryDays(everyDays - 1) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(recurrenceLabel(Recurrence.EveryDays(everyDays)), modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedIconButton(onClick = { if (everyDays < Recurrence.MAX_DAYS) onEveryDays(everyDays + 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        RepeatOption(stringResource(R.string.editor_weekdays), kind == RepeatKind.WEEKLY) { onKind(RepeatKind.WEEKLY) }
        if (kind == RepeatKind.WEEKLY) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 40.dp)) {
                WEEK.forEach { day ->
                    FilterChip(
                        selected = day in weekDays,
                        onClick = { onWeekDays(if (day in weekDays) weekDays - day else weekDays + day) },
                        label = { Text(day.shortName()) },
                    )
                }
            }
        }
        RepeatOption(stringResource(R.string.editor_monthly), kind == RepeatKind.MONTHLY) { onKind(RepeatKind.MONTHLY) }
        if (kind == RepeatKind.MONTHLY) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 40.dp)) {
                OutlinedIconButton(onClick = { onMonthDay(if (monthDay > 1) monthDay - 1 else 31) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(stringResource(R.string.editor_month_day, ordinal(monthDay)), modifier = Modifier.padding(horizontal = 16.dp))
                OutlinedIconButton(onClick = { onMonthDay(if (monthDay < 31) monthDay + 1 else 1) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        if (kind != RepeatKind.ONCE) {
            Text(stringResource(R.string.editor_starts), style = MaterialTheme.typography.labelLarge)
            if (kind == RepeatKind.MONTHLY) {
                // Monthly starts on the next of its days; nothing to choose.
                Text(stringResource(R.string.editor_starts_on, dayLabel(firstDue, today)), modifier = Modifier.padding(start = 4.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(today, today.plusDays(1)).forEach { date ->
                        FilterChip(selected = dueDate == date, onClick = { onDueDate(date) }, label = { Text(dayLabel(date, today)) })
                    }
                    if (firstDue != today && firstDue != today.plusDays(1)) {
                        FilterChip(selected = true, onClick = {}, label = { Text(stringResource(R.string.editor_starts_on, dayLabel(firstDue, today))) })
                    }
                }
            }
            // "Until the end of next year": the last day it can be due.
            Text(stringResource(R.string.editor_ends), style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = repeatUntil == null, onClick = { onRepeatUntil(null) }, label = { Text(stringResource(R.string.editor_ends_never)) })
                FilterChip(
                    selected = repeatUntil != null,
                    onClick = { pickingEnd = true },
                    label = { Text(repeatUntil?.let { stringResource(R.string.editor_ends_on, endLabel(it)) } ?: stringResource(R.string.editor_ends_pick)) },
                )
            }
        }
    }

    if (pickingEnd) {
        val start = maxOf(firstDue, today)
        val utcStart = start.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = (repeatUntil ?: start.withDayOfYear(1).plusYears(2).minusDays(1))
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= utcStart
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingEnd = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        picker.selectedDateMillis?.let { onRepeatUntil(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                        pickingEnd = false
                    },
                    enabled = picker.selectedDateMillis != null,
                ) { Text(stringResource(CoreUiR.string.save)) }
            },
            dismissButton = { TextButton(onClick = { pickingEnd = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        ) { DatePicker(state = picker) }
    }
}

/** "31 Dec 2027". */
private fun endLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()))

@Composable
internal fun currentRecurrenceLabel(kind: RepeatKind, everyDays: Int, weekDays: Set<DayOfWeek>, monthDay: Int): String = when (kind) {
    RepeatKind.ONCE -> recurrenceLabel(Recurrence.Once)
    RepeatKind.EVERY_DAYS -> recurrenceLabel(Recurrence.EveryDays(everyDays))
    RepeatKind.WEEKLY -> if (weekDays.isEmpty()) recurrenceLabel(Recurrence.Once) else recurrenceLabel(Recurrence.Weekly(weekDays))
    RepeatKind.MONTHLY -> recurrenceLabel(Recurrence.Monthly(monthDay))
}

@Composable
private fun RepeatOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

internal fun Recurrence.kind() = when (this) {
    Recurrence.Once -> RepeatKind.ONCE
    is Recurrence.EveryDays -> RepeatKind.EVERY_DAYS
    is Recurrence.Weekly -> RepeatKind.WEEKLY
    is Recurrence.Monthly -> RepeatKind.MONTHLY
}
