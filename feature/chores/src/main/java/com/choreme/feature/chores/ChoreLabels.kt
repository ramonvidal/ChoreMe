package com.choreme.feature.chores

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale


internal fun DayOfWeek.shortName(): String = getDisplayName(TextStyle.SHORT, Locale.getDefault())

/** Monday first, as on most calendars outside the US. */
internal val WEEK: List<DayOfWeek> = DayOfWeek.entries

@Composable
internal fun recurrenceLabel(recurrence: Recurrence): String = when (recurrence) {
    Recurrence.Once -> stringResource(R.string.repeat_once)
    is Recurrence.EveryDays -> pluralStringResource(R.plurals.repeat_every_days, recurrence.days, recurrence.days)
    is Recurrence.Weekly -> stringResource(
        R.string.repeat_weekly,
        WEEK.filter { it in recurrence.days }.joinToString { it.shortName() },
    )
    is Recurrence.Monthly -> stringResource(R.string.repeat_monthly, ordinal(recurrence.day))
}

/** "1st", "2nd", "3rd", "5th", "11th", "22nd" in English; just the number elsewhere ("dia 5"). */
internal fun ordinal(day: Int): String {
    if (Locale.getDefault().language != "en") return day.toString()
    val suffix = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

/** "until 31 Dec 2027". */
@Composable
internal fun untilLabel(until: LocalDate): String =
    stringResource(R.string.repeat_until, until.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())))

/** "Today", "Tomorrow", "Wed", or a date further out. */
@Composable
internal fun dayLabel(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> stringResource(R.string.due_today)
        days == 1L -> stringResource(R.string.due_tomorrow)
        days in 2..6 -> date.dayOfWeek.shortName()
        else -> date.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    }
}
