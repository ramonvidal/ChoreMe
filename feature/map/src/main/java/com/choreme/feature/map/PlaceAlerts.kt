package com.choreme.feature.map

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * "Leo arrived at School" on every phone in the family. The free Firebase plan can't push,
 * so each phone checks about every 15 minutes (the most often Android allows).
 */
object PlaceAlerts {
    private const val WORK_NAME = "place-alerts"

    /** Safe to call on every app start: an existing schedule is kept. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PlaceAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
