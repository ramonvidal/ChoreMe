package com.choreme.core.model.chore

import java.time.DayOfWeek
import java.time.LocalDate

sealed interface Recurrence {
    data object Once : Recurrence

    /** Every [days] days; 1 is daily. */
    data class EveryDays(val days: Int) : Recurrence {
        init {
            require(days in 1..MAX_DAYS) { "Repeat every 1 to $MAX_DAYS days" }
        }
    }

    /** On the given weekdays, e.g. Monday and Thursday. */
    data class Weekly(val days: Set<DayOfWeek>) : Recurrence {
        init {
            require(days.isNotEmpty()) { "Pick at least one day" }
        }
    }

    /**
     * On this day of every month, e.g. the 5th. In a shorter month a 29th to 31st falls on its
     * last day, so "the 31st" is never skipped.
     */
    data class Monthly(val day: Int) : Recurrence {
        init {
            require(day in 1..31) { "Pick a day from 1 to 31" }
        }

        /** This month's (or a later month's) date for [day], on or after [date]. */
        fun onOrAfter(date: LocalDate): LocalDate {
            val thisMonth = date.withDayOfMonth(minOf(day, date.lengthOfMonth()))
            if (!thisMonth.isBefore(date)) return thisMonth
            val next = date.plusMonths(1)
            return next.withDayOfMonth(minOf(day, next.lengthOfMonth()))
        }
    }

    companion object {
        const val MAX_DAYS = 90
    }
}

/**
 * The next date after a chore is done, or null for one-off chores. Counts from whichever
 * is later, the due date or the day it was done, so doing a chore early doesn't bring
 * the next one forward and doing it late doesn't leave it overdue.
 */
fun Recurrence.nextDueDate(previousDue: LocalDate, doneOn: LocalDate): LocalDate? {
    val from = maxOf(previousDue, doneOn)
    return when (this) {
        Recurrence.Once -> null
        is Recurrence.EveryDays -> from.plusDays(days.toLong())
        is Recurrence.Weekly -> generateSequence(from.plusDays(1)) { it.plusDays(1) }.first { it.dayOfWeek in days }
        is Recurrence.Monthly -> onOrAfter(from.plusDays(1))
    }
}

/** The first due date for a new repeating chore that starts on or after [start]. */
fun Recurrence.firstDueDate(start: LocalDate): LocalDate = when (this) {
    is Recurrence.Monthly -> onOrAfter(start)
    is Recurrence.Weekly -> generateSequence(start) { it.plusDays(1) }.first { it.dayOfWeek in days }
    else -> start
}
