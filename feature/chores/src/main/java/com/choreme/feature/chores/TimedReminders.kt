package com.choreme.feature.chores

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.model.session.SessionState
import com.choreme.core.settings.NotificationSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "Remind me at 18:00" on a chore: this phone only, never shared. One per chore; setting
 * a new time replaces the old one. Android may deliver it a few minutes late to save battery.
 */
@Singleton
internal class TimedReminders @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("timed-reminders", Context.MODE_PRIVATE)
    private val _all = MutableStateFlow(load())

    /** Chore id to when its reminder is due. */
    val all: StateFlow<Map<String, Instant>> = _all.asStateFlow()

    fun set(choreId: String, title: String, at: Instant) {
        val delay = Duration.between(Instant.now(), at).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<TimedReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_CHORE to choreId, KEY_TITLE to title))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(workName(choreId), ExistingWorkPolicy.REPLACE, request)
        save(_all.value + (choreId to at))
    }

    fun cancel(choreId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName(choreId))
        save(_all.value - choreId)
    }

    /** Once it has gone off. */
    fun done(choreId: String) = save(_all.value - choreId)

    private fun load(): Map<String, Instant> {
        val now = Instant.now()
        return prefs.all.mapNotNull { (id, millis) -> (millis as? Long)?.let { id to Instant.ofEpochMilli(it) } }
            .filter { it.second.isAfter(now) }
            .toMap()
    }

    private fun save(value: Map<String, Instant>) {
        _all.value = value
        prefs.edit().clear().apply { value.forEach { (id, at) -> putLong(id, at.toEpochMilli()) } }.apply()
    }

    private fun workName(choreId: String) = "remind-$choreId"

    companion object {
        const val KEY_CHORE = "chore"
        const val KEY_TITLE = "title"
    }
}

@HiltWorker
internal class TimedReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val chores: ChoreRepository,
    private val notifications: NotificationSettings,
    private val reminders: TimedReminders,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val choreId = inputData.getString(TimedReminders.KEY_CHORE) ?: return Result.success()
        reminders.done(choreId)
        var title = inputData.getString(TimedReminders.KEY_TITLE).orEmpty()
        // Skip it if the chore was done or removed meanwhile; without a connection, remind anyway.
        if (availability.isConfigured) {
            val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
                as? SessionState.InFamily
            if (session != null) {
                val chore = withTimeoutOrNull(TIMEOUT_MS) { chores.observeChores(session.familyId).first() }
                    ?.let { list -> list.firstOrNull { it.id == choreId } ?: return Result.success() }
                if (chore != null) {
                    if (!chore.isActive) return Result.success()
                    title = chore.title
                }
            }
        }
        if (canNotify()) show(choreId, title)
        return Result.success()
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(choreId: String, title: String) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.timed_channel), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.timed_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_chores)
            .setContentTitle(context.getString(R.string.timed_title, title))
            .setContentText(context.getString(R.string.timed_body))
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify("remind-$choreId".hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "timed-reminders"
        const val TIMEOUT_MS = 20_000L
    }
}
