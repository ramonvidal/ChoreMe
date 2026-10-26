package com.choreme.feature.map

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
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choreme.core.data.AnnouncementRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.model.AlertKind
import com.choreme.core.model.Announcement
import com.choreme.core.model.AnnouncementKind
import com.choreme.core.model.key
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.toNotify
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushListener
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
import javax.inject.Inject

/**
 * "Leo is on the way home" on the family's other phones. Only phones with ntfy get it as a
 * notification (it's news for the next hour); everyone sees it on the Map tab.
 * Same shape as the grocery list's "at the shop" alert, each feature posting its own news.
 */
internal class OnMyWayListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind != PushKinds.ON_MY_WAY) return
        val request = OneTimeWorkRequestBuilder<OnMyWayAlertWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("on-my-way-now", ExistingWorkPolicy.REPLACE, request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class OnMyWayListenerModule {
    @Binds
    @IntoSet
    abstract fun onMyWay(listener: OnMyWayListener): PushListener
}

@HiltWorker
internal class OnMyWayAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val announcements: AnnouncementRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences("on-my-way-alerts", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        if (!availability.isConfigured || !notifications.isEnabled(AlertKind.ON_MY_WAY) || !canNotify()) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        return try {
            val all = announcements.all(session.familyId)
            val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
            val fresh = all.toNotify(AnnouncementKind.ON_MY_WAY, session.memberId, Instant.now(), shown)
            if (fresh.isNotEmpty()) {
                val names = families.observeMembers(session.familyId).first().associate { it.id to it.name }
                fresh.forEach { show(it, names[it.memberId]) }
            }
            prefs.edit().putStringSet(KEY_SHOWN, all.map { it.key }.toSet()).apply()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(announcement: Announcement, name: String?) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.on_my_way_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.on_my_way_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_place)
            .setContentTitle(context.getString(R.string.on_my_way_title, name ?: context.getString(R.string.someone)))
            .setWhen(announcement.at.toEpochMilli())
            .setShowWhen(true)
            .setTimeoutAfter(announcement.kind.lasts.toMillis())
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify("on-my-way-${announcement.memberId}".hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "on-my-way"
        const val KEY_SHOWN = "shown"
        const val TIMEOUT_MS = 30_000L
    }
}
