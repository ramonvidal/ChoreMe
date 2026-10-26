package com.choreme.core.model.chore

import java.time.LocalDateTime
import java.time.LocalTime

/**
 * "Remind me at 18:00" means the next 18:00: today if it's still ahead, otherwise tomorrow.
 * Reminders are this phone's own and never shared with the family.
 */
fun nextReminderTime(now: LocalDateTime, at: LocalTime): LocalDateTime {
    val today = now.toLocalDate().atTime(at)
    return if (today.isAfter(now)) today else today.plusDays(1)
}
