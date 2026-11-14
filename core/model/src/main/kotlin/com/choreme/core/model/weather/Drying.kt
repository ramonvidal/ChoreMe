package com.choreme.core.model.weather

import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import java.time.LocalDate

/** How good a day is for drying washing outside. */
enum class DryingQuality { GOOD, OK, POOR }

/**
 * From the daytime [hours] (say 9:00 to 17:00): dry air, some wind, mild warmth and no rain make a good
 * day. Returns [DryingQuality.POOR] with no forecast.
 */
fun dryingQuality(hours: List<WeatherHour>): DryingQuality {
    if (hours.isEmpty()) return DryingQuality.POOR
    val rain = hours.maxOf { it.rainProbability }
    val rainMm = hours.sumOf { it.rainMm }
    val humidity = hours.map { it.humidity }.average()
    val temp = hours.map { it.temperatureC }.average()
    val wind = hours.map { it.windKmh }.average()
    return when {
        rain >= 50 || rainMm >= 1.0 || temp < 5.0 -> DryingQuality.POOR
        rain < 20 && humidity <= 70 && temp >= 12.0 && wind >= 6.0 -> DryingQuality.GOOD
        rain < 35 && humidity <= 85 && temp >= 8.0 -> DryingQuality.OK
        else -> DryingQuality.POOR
    }
}

/** The laundry is waiting: a laundry chore that is due today (or late) and not done. */
fun laundryDue(chores: List<Chore>, today: LocalDate): Boolean =
    chores.any { it.template == ChoreTemplate.LAUNDRY && it.status != ChoreStatus.DONE && !it.dueDate.isAfter(today) }

/** What to tell the family about hanging the washing out. */
sealed interface DryingTip {
    /** Today is a good day: hang it out. */
    data object HangToday : DryingTip

    /** Not today, but [date] looks good. */
    data class WaitFor(val date: LocalDate) : DryingTip

    /** No good day in sight: dry it indoors. */
    data object Indoors : DryingTip
}

/** How many days ahead to look for a better day. */
const val DRYING_LOOKAHEAD = 3

/**
 * A tip only while the laundry is waiting ([laundryDue]). [days] starts with today. Today good: hang it out. Today
 * poor with a good day in the next three: wait for it. Today poor and none good: dry it indoors. An ordinary
 * "OK" day says nothing.
 */
fun dryingTip(laundryDue: Boolean, days: List<DayOutlook>): DryingTip? {
    if (!laundryDue || days.isEmpty()) return null
    val today = days.first()
    if (today.drying == DryingQuality.GOOD) return DryingTip.HangToday
    val better = days.drop(1).take(DRYING_LOOKAHEAD).firstOrNull { it.drying == DryingQuality.GOOD }
    return when {
        better != null -> DryingTip.WaitFor(better.date)
        today.drying == DryingQuality.POOR -> DryingTip.Indoors
        else -> null
    }
}
