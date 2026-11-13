package com.choreme.feature.home

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

/** The morning weather notification: early every day, posted by each phone itself. Scheduled by `:app` at startup. */
object WeatherAlerts {
    private const val WORK_NAME = "weather-morning"
    private val MORNING: LocalTime = LocalTime.of(7, 0)

    /** Safe to call on every app start: an existing schedule is kept. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WeatherMorningWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(untilNext(MORNING).toMillis(), TimeUnit.MILLISECONDS)
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
