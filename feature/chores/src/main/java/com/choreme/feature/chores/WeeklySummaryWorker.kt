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
import androidx.work.WorkerParameters
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.model.AlertKind
import com.choreme.core.model.chore.WeekRecap
import com.choreme.core.model.chore.completeLog
import com.choreme.core.model.chore.weekRecap
import com.choreme.core.model.chore.weekStart
import com.choreme.core.model.session.SessionState
import com.choreme.core.settings.NotificationSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * "This week: 23 points · Mia 9, Leo 7, Ramon 5", on Sunday evening. Runs every evening (see
 * [ChoreReminders]) and only posts on Sundays, so a phone that was off on Sunday stays quiet.
 */
@HiltWorker
internal class WeeklySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val chores: ChoreRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfWeek != DayOfWeek.SUNDAY) return Result.success()
        if (!availability.isConfigured || !canNotify() || !notifications.isEnabled(AlertKind.WEEKLY_SUMMARY)) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val members = withTimeoutOrNull(TIMEOUT_MS) { families.observeMembers(session.familyId).first() } ?: return Result.retry()
        val from = weekStart(today)
        val log = withTimeoutOrNull(TIMEOUT_MS) { chores.observeLog(session.familyId, from).first() } ?: return Result.retry()
        val allChores = withTimeoutOrNull(TIMEOUT_MS) { chores.observeChores(session.familyId).first() } ?: return Result.retry()
        // Without points, the recap only says how many chores got done: no numbers per person.
        val pointsOn = withTimeoutOrNull(TIMEOUT_MS) { families.observeFamily(session.familyId).first() }?.pointsEnabled ?: true
        weekRecap(completeLog(log, allChores), members, today)?.let { show(it, pointsOn) }
        return Result.success()
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(recap: WeekRecap, pointsOn: Boolean) {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.weekly_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.weekly_channel_description) },
        )
        val title = if (pointsOn) {
            context.resources.getQuantityString(R.plurals.weekly_title, recap.totalPoints, recap.totalPoints)
        } else {
            context.resources.getQuantityString(R.plurals.weekly_chores_title, recap.chores, recap.chores)
        }
        val text = if (pointsOn) {
            recap.byPerson.joinToString(", ") { (name, points) -> context.getString(R.string.weekly_person, name, points) }
        } else {
            context.getString(R.string.weekly_chores_body)
        }

        // Opens the app without this module having to know its activity.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_chores)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG.hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "weekly-summary"
        const val NOTIFICATION_TAG = "weekly-summary"
        const val TIMEOUT_MS = 30_000L
    }
}
