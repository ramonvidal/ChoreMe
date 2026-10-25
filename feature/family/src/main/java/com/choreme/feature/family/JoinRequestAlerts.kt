package com.choreme.feature.family

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
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.model.Role
import com.choreme.core.model.session.SessionState
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushListener
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
import javax.inject.Inject

/**
 * "Bob wants to join the family" for parents, as soon as the request comes in (phones with
 * ntfy). Without ntfy, parents see the badge on the Family tab next time they open the app.
 */
internal class JoinRequestListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind != PushKinds.JOIN_REQUEST) return
        val request = OneTimeWorkRequestBuilder<JoinRequestWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("join-requests-now", ExistingWorkPolicy.REPLACE, request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class JoinRequestListenerModule {
    @Binds
    @IntoSet
    abstract fun joinRequests(listener: JoinRequestListener): PushListener
}

@HiltWorker
internal class JoinRequestWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences("join-requests", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        if (!availability.isConfigured) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        if (session.role != Role.PARENT) return Result.success()
        val requests = withTimeoutOrNull(TIMEOUT_MS) { families.observeJoinRequests(session.familyId).first() }
            ?: return Result.retry()
        val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
        if (canNotify() && notifications.isEnabled(AlertKind.JOIN_REQUESTS)) requests.filter { it.uid !in shown }.forEach { show(it.uid, it.name) }
        prefs.edit().putStringSet(KEY_SHOWN, requests.map { it.uid }.toSet()).apply()
        return Result.success()
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(uid: String, name: String) {
        val context = applicationContext
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.join_channel), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.join_channel_description) },
        )
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_family)
            .setContentTitle(context.getString(R.string.join_title, name))
            .setContentText(context.getString(R.string.join_body))
            .setAutoCancel(true)
            .setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
            .build()
        NotificationManagerCompat.from(context).notify("join-$uid".hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "join-requests"
        const val KEY_SHOWN = "shown"
        const val TIMEOUT_MS = 30_000L
    }
}
