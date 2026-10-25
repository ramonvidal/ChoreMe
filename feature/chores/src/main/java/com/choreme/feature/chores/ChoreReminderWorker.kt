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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.model.chore.ChoreReminder
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.chore.reminderFor
import com.choreme.core.model.session.SessionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

@HiltWorker
internal class ChoreReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val chores: ChoreRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!availability.isConfigured || !canNotify() || !notifications.isEnabled(AlertKind.MORNING_SUMMARY)) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val allChores = withTimeoutOrNull(TIMEOUT_MS) { chores.observeChores(session.familyId).first() }
            ?: return Result.retry()

        val today = LocalDate.now()
        reminderFor(buildTodayBoard(allChores, today), session.role, today)?.let(::show)
        return Result.success()
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(reminder: ChoreReminder) {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.reminder_channel_description) },
        )

        val res = context.resources
        val title = buildList {
            if (reminder.toDo > 0) add(res.getQuantityString(R.plurals.reminder_to_do, reminder.toDo, reminder.toDo))
            if (reminder.overdue > 0) add(res.getQuantityString(R.plurals.reminder_overdue, reminder.overdue, reminder.overdue))
            if (reminder.awaitingApproval > 0) {
                add(res.getQuantityString(R.plurals.reminder_awaiting, reminder.awaitingApproval, reminder.awaitingApproval))
            }
        }.joinToString(" · ")

        // Opens the app without this module having to know its activity.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_chores)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.reminder_body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "chore-reminders"
        const val NOTIFICATION_ID = 1
        const val TIMEOUT_MS = 30_000L
    }
}
