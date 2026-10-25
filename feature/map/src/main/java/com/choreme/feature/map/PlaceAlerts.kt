package com.choreme.feature.map

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * "Leo arrived at School" on every phone in the family. Phones with a UnifiedPush app such
 * as ntfy get pinged and check at once; every phone also checks about every 15 minutes
 * (the most often Android allows), which catches anything a ping missed.
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

    /** Checks right away, after a ping from another phone. */
    internal fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<PlaceAlertWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private const val NOW_WORK_NAME = "place-alerts-now"
}
