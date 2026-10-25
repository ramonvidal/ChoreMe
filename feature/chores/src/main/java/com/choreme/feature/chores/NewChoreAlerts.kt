package com.choreme.feature.chores

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.choreme.core.settings.NotificationSettings
import com.choreme.core.model.AlertKind
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.model.Role
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.session.SessionState
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushListener
import com.choreme.core.push.PushRegistration
import com.choreme.core.ui.emoji
import dagger.Binds
import dagger.Module
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * "Leo noticed: Wash dishes" on the family's other phones. Phones with ntfy are pinged and
 * check at once; a background check catches anything missed (rarely when pings work).
 */
internal object NewChoreAlerts {
    private const val PERIODIC = "new-chores"
    private const val NOW = "new-chores-now"
    private const val MINUTES_WITHOUT_PINGS = 30L
    private const val MINUTES_WITH_PINGS = 120L

    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context) {
        val minutes = if (PushRegistration.isAvailable(context)) MINUTES_WITH_PINGS else MINUTES_WITHOUT_PINGS
        val request = PeriodicWorkRequestBuilder<NewChoreWorker>(minutes, TimeUnit.MINUTES).setConstraints(connected).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<NewChoreWorker>().setConstraints(connected).build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }
}

internal class NewChoreListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind in setOf(PushKinds.CHORE_ADDED, PushKinds.CHORE_TO_APPROVE, PushKinds.CHORE_DONE)) {
            NewChoreAlerts.checkNow(context)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NewChoreListenerModule {
    @Binds
    @IntoSet
    abstract fun newChores(listener: NewChoreListener): PushListener
}

@HiltWorker
internal class NewChoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val chores: ChoreRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences("new-chores", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        if (!availability.isConfigured) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val now = Instant.now()
        val since = prefs.getLong(KEY_CHECKED, 0L).takeIf { it > 0 }?.let(Instant::ofEpochMilli)
        if (since == null) {
            // First run on this phone: don't announce every chore already on the list.
            prefs.edit().putLong(KEY_CHECKED, now.toEpochMilli()).apply()
            return Result.success()
        }
        return try {
            val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
            // Look back a little: a chore added offline reaches the server later than it was made.
            val recent = chores.reportedSince(session.familyId, since.minus(LOOK_BACK))
            val fresh = recent.filter { it.id !in shown && it.reportedBy != session.memberId && it.isActive }
            if (fresh.isNotEmpty() && canNotify() && notifications.isEnabled(AlertKind.NEW_CHORES)) {
                val names = families.observeMembers(session.familyId).first().associate { it.id to it.name }
                fresh.forEach { show(it, names[it.reportedBy]) }
            }
            prefs.edit()
                .putLong(KEY_CHECKED, now.toEpochMilli())
                .putStringSet(KEY_SHOWN, recent.map { it.id }.toSet())
                .apply()
            showDone(session.familyId, session.memberId, since.minus(LOOK_BACK))
            if (session.role == Role.PARENT) showApprovals(session.familyId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    /** "Leo did: Wash dishes" for chores others finished, if the chore asks to tell everyone. */
    private suspend fun showDone(familyId: String, memberId: String, since: Instant) {
        val today = java.time.LocalDate.now()
        val done = chores.changedSince(familyId, since)
            .filter { it.notifyWhenDone && it.lastDoneOn == today && it.lastDoneBy != null && it.lastDoneBy != memberId }
        // Keyed by who and when, so a daily chore done again tomorrow announces again.
        val key = { chore: Chore -> "${chore.id}|${chore.lastDoneBy}|${chore.lastDoneOn}" }
        val shown = prefs.getStringSet(KEY_DONE_SHOWN, emptySet()).orEmpty()
        val fresh = done.filter { key(it) !in shown }
        if (fresh.isNotEmpty() && canNotify() && notifications.isEnabled(AlertKind.CHORES_DONE)) {
            val names = families.observeMembers(familyId).first().associate { it.id to it.name }
            fresh.forEach { showDoneNotification(it, names[it.lastDoneBy]) }
        }
        prefs.edit().putStringSet(KEY_DONE_SHOWN, (shown + fresh.map(key)).takeLastKeys()).apply()
    }

    @Suppress("MissingPermission") // Checked by the caller.
    private fun showDoneNotification(chore: Chore, who: String?) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(DONE_CHANNEL_ID, context.getString(R.string.done_channel), NotificationManager.IMPORTANCE_LOW)
                .apply { description = context.getString(R.string.done_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, DONE_CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_chores)
            .setContentTitle(context.getString(R.string.done_title, who ?: context.getString(R.string.someone), chore.title))
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify("done-${chore.id}".hashCode(), notification)
    }

    /** Kids' finished chores waiting for this parent's OK, each shown once. */
    private suspend fun showApprovals(familyId: String) {
        val waiting = chores.observeChores(familyId).first().filter { it.status == ChoreStatus.PENDING_APPROVAL }
        val shown = prefs.getStringSet(KEY_APPROVALS_SHOWN, emptySet()).orEmpty()
        // Keyed by chore and who did it, so the same chore finished again next week alerts again.
        val key = { chore: Chore -> "${chore.id}|${chore.completedBy}|${chore.dueDate}" }
        val fresh = waiting.filter { key(it) !in shown }
        if (fresh.isNotEmpty() && canNotify() && notifications.isEnabled(AlertKind.APPROVALS)) {
            val names = families.observeMembers(familyId).first().associate { it.id to it.name }
            fresh.forEach { showApproval(it, names[it.completedBy]) }
        }
        prefs.edit().putStringSet(KEY_APPROVALS_SHOWN, waiting.map(key).toSet()).apply()
    }

    @Suppress("MissingPermission") // Checked by the caller.
    private fun showApproval(chore: Chore, who: String?) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(APPROVAL_CHANNEL_ID, context.getString(R.string.approval_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.approval_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, APPROVAL_CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_chores)
            .setContentTitle(context.getString(R.string.approval_title, who ?: context.getString(R.string.someone), chore.title))
            .setContentText(context.getString(R.string.approval_body))
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify("approve-${chore.id}".hashCode(), notification)
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(chore: Chore, who: String?) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.new_chore_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.new_chore_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_chores)
            .setContentTitle("${chore.template.emoji} ${chore.title}")
            .setContentText(context.getString(R.string.new_chore_body, who ?: context.getString(R.string.someone)))
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify(chore.id.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "new-chores"
        const val KEY_CHECKED = "checked_until"
        const val KEY_SHOWN = "shown"
        const val KEY_APPROVALS_SHOWN = "approvals_shown"
        const val KEY_DONE_SHOWN = "done_shown"
        const val DONE_CHANNEL_ID = "chores-done"
        const val APPROVAL_CHANNEL_ID = "approvals"
        const val TIMEOUT_MS = 30_000L
        val LOOK_BACK: Duration = Duration.ofHours(1)
    }
}

/** Keeps today's and yesterday's "done" keys; older ones can't come back (they're filtered by date). */
private fun Set<String>.takeLastKeys(): Set<String> {
    val recent = setOf(java.time.LocalDate.now().toString(), java.time.LocalDate.now().minusDays(1).toString())
    return filter { key -> recent.any { key.endsWith(it) } }.toSet()
}
