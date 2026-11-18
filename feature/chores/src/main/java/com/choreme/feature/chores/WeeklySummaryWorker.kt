package com.choreme.feature.chores

import com.choreme.core.settings.AlertChannel
import com.choreme.core.settings.Notifier
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
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
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfWeek != DayOfWeek.SUNDAY) return Result.success()
        if (!availability.isConfigured || !notifier.canNotify() || !notifications.isEnabled(AlertKind.WEEKLY_SUMMARY)) return Result.success()
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

    private fun show(recap: WeekRecap, pointsOn: Boolean) {
        val context = applicationContext
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
        notifier.show(CHANNEL, NOTIFICATION_TAG.hashCode()) {
            setSmallIcon(R.drawable.ic_notification_chores)
            setContentTitle(title)
            setContentText(text)
            setStyle(NotificationCompat.BigTextStyle().bigText(text))
            setContentIntent(contentIntent)
            setAutoCancel(true)
        }
    }

    private companion object {
        val CHANNEL = AlertChannel(CHANNEL_ID, R.string.weekly_channel, R.string.weekly_channel_description, NotificationManager.IMPORTANCE_DEFAULT)
        const val CHANNEL_ID = "weekly-summary"
        const val NOTIFICATION_TAG = "weekly-summary"
        const val TIMEOUT_MS = 30_000L
    }
}
