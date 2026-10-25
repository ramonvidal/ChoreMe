package com.choreme.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.agenda.monthGrid
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** A month at a glance: a dot per event (in its calendar's color) and one for chores. Tap a day to see it below. */
@Composable
internal fun MonthView(
    state: AgendaUiState,
    onMonth: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onOpenEvent: (CalendarEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val day = state.selectedDayEntries
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)) {
        item(key = "header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = { onMonth(state.month.minusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.previous_month))
                }
                Text(
                    state.month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onMonth(state.month.plusMonths(1)) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.next_month))
                }
            }
        }
        item(key = "weekdays") {
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { weekday ->
                    Text(
                        weekday.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item(key = "grid") {
            Column {
                monthGrid(state.month).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            DayCell(
                                date = date,
                                inMonth = YearMonth.from(date) == state.month,
                                isToday = date == state.today,
                                isSelected = date == state.selectedDay,
                                eventColors = state.monthDays[date]?.events?.map { it.color }.orEmpty(),
                                hasChores = state.monthDays[date]?.chores?.isNotEmpty() == true,
                                onClick = { onSelect(date) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        item(key = "selected-day") {
            Text(
                state.selectedDay.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 4.dp),
            )
        }
        if (day == null || (day.events.isEmpty() && day.chores.isEmpty())) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.nothing_this_day),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(4.dp),
                )
            }
        } else {
            items(day.events, key = { "e-${it.id}-${it.start}" }) { event -> EventRow(event, onClick = { onOpenEvent(event) }) }
            items(day.chores, key = { "c-${it.id}" }) { chore -> ChoreRow(chore) }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    eventColors: List<Int>,
    hasChores: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
            Text(
                date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (inMonth) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(top = 2.dp)) {
                eventColors.take(MAX_DOTS).forEach { Dot(Color(it)) }
                if (hasChores) Dot(MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(5.dp).background(color, CircleShape))
}

private const val MAX_DOTS = 3
