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

@get:StringRes
internal val ChoreTemplate.title: Int
    get() = when (this) {
        ChoreTemplate.DISHES -> R.string.template_dishes
        ChoreTemplate.LAUNDRY -> R.string.template_laundry
        ChoreTemplate.FOLD_CLOTHES -> R.string.template_fold_clothes
        ChoreTemplate.CLEAN_KITCHEN -> R.string.template_clean_kitchen
        ChoreTemplate.CLEAN_BATHROOM -> R.string.template_clean_bathroom
        ChoreTemplate.TRASH -> R.string.template_trash
        ChoreTemplate.VACUUM -> R.string.template_vacuum
        ChoreTemplate.MAKE_BEDS -> R.string.template_make_beds
        ChoreTemplate.WATER_PLANTS -> R.string.template_water_plants
        ChoreTemplate.GROCERIES -> R.string.template_groceries
        ChoreTemplate.OTHER -> R.string.template_other
    }

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
}

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
