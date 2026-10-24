package com.choreme.feature.chores

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Daily morning summary of the chores due, posted by each phone itself. The free Firebase
 * plan can't send pushes from a server, so this is how reminders work for now (see PLAN.md).
 */
object ChoreReminders {
    private const val WORK_NAME = "chore-morning-reminder"
    private val REMINDER_TIME: LocalTime = LocalTime.of(8, 0)

    /** Safe to call on every app start: an existing schedule is kept. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ChoreReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(untilNext(REMINDER_TIME).toMillis(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun untilNext(time: LocalTime): Duration {
        val now = LocalDateTime.now()
        val today = now.toLocalDate().atTime(time)
        return Duration.between(now, if (now.isBefore(today)) today else today.plusDays(1))
    }
}
