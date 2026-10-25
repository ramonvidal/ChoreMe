package com.choreme.feature.map

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.choreme.core.push.PushRegistration
import java.util.concurrent.TimeUnit

/**
 * "Leo arrived at School" on every phone in the family. Phones with a UnifiedPush app such
 * as ntfy get pinged and check at once, so their background check is only a rare safety
 * net; phones without one check about every 15 minutes (the most often Android allows).
 */
object PlaceAlerts {
    private const val WORK_NAME = "place-alerts"
    private const val MINUTES_WITHOUT_PINGS = 15L
    private const val MINUTES_WITH_PINGS = 60L

    /** Safe to call on every app start: keeps the schedule, adjusting it if ntfy was installed or removed. */
    fun schedule(context: Context) {
        val minutes = if (PushRegistration.isAvailable(context)) MINUTES_WITH_PINGS else MINUTES_WITHOUT_PINGS
        val request = PeriodicWorkRequestBuilder<PlaceAlertWorker>(minutes, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
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
