package com.choreme.feature.family

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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.MessageRepository
import com.choreme.core.model.AlertKind
import com.choreme.core.model.FamilyMessage
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.toNotify
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushListener
import com.choreme.core.push.PushRegistration
import com.choreme.core.settings.NotificationSettings
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
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * "Mom: Dinner's ready!" on the family's other phones. Phones with ntfy are pinged and show it
 * at once; others check in the background (set up the first time this phone opens the Family tab).
 */
internal object MessageAlerts {
    private const val PERIODIC = "family-messages"
    private const val NOW = "family-messages-now"
    private const val MINUTES_WITHOUT_PINGS = 30L
    private const val MINUTES_WITH_PINGS = 120L

    private val connected = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun schedule(context: Context) {
        val minutes = if (PushRegistration.isAvailable(context)) MINUTES_WITH_PINGS else MINUTES_WITHOUT_PINGS
        val request = PeriodicWorkRequestBuilder<MessageWorker>(minutes, TimeUnit.MINUTES).setConstraints(connected).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<MessageWorker>().setConstraints(connected).build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.REPLACE, request)
    }
}

internal class MessageListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind == PushKinds.MESSAGE) MessageAlerts.checkNow(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class MessageListenerModule {
    @Binds
    @IntoSet
    abstract fun messages(listener: MessageListener): PushListener
}

@HiltWorker
internal class MessageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val messages: MessageRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences("family-messages", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        if (!availability.isConfigured) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val now = Instant.now()
        return try {
            val recent = messages.since(session.familyId, now.minus(FamilyMessage.NOTIFY_WITHIN))
            val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
            val fresh = recent.toNotify(session.memberId, now, shown)
            if (fresh.isNotEmpty() && canNotify() && notifications.isEnabled(AlertKind.MESSAGES)) {
                val names = families.observeMembers(session.familyId).first().associate { it.id to it.name }
                fresh.forEach { show(it, names[it.from]) }
            }
            // Older ones can't come back: they're past the notify window.
            prefs.edit().putStringSet(KEY_SHOWN, recent.map { it.id }.toSet()).apply()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(message: FamilyMessage, name: String?) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.message_channel), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.message_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_family)
            .setContentTitle(name ?: context.getString(R.string.message_someone))
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setWhen(message.at.toEpochMilli())
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify("message-${message.id}".hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "family-messages"
        const val KEY_SHOWN = "shown"
        const val TIMEOUT_MS = 30_000L
    }
}
