package com.choreme.feature.groceries

import com.choreme.core.ui.R as CoreUiR
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.choreme.core.ui.DeepLinks
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
 * "Mom is at the shop" on the family's other phones. Only phones with ntfy get it as a
 * notification (it's news for the next hour or two); everyone sees it on the grocery list.
 */
internal class ShoppingListener @Inject constructor(@ApplicationContext private val context: Context) : PushListener {
    override fun onPing(kind: String) {
        if (kind != PushKinds.SHOPPING) return
        val request = OneTimeWorkRequestBuilder<ShoppingAlertWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("shopping-now", ExistingWorkPolicy.REPLACE, request)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShoppingListenerModule {
    @Binds
    @IntoSet
    abstract fun shopping(listener: ShoppingListener): PushListener
}

@HiltWorker
internal class ShoppingAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val announcements: AnnouncementRepository,
    private val notifications: NotificationSettings,
) : CoroutineWorker(context, params) {
    private val prefs = context.getSharedPreferences("shopping-alerts", Context.MODE_PRIVATE)

    override suspend fun doWork(): Result {
        if (!availability.isConfigured || !notifications.isEnabled(AlertKind.SHOPPING) || !canNotify()) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        return try {
            val all = announcements.all(session.familyId)
            val shown = prefs.getStringSet(KEY_SHOWN, emptySet()).orEmpty()
            val fresh = all.toNotify(AnnouncementKind.SHOPPING, session.memberId, Instant.now(), shown)
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
            NotificationChannel(CHANNEL_ID, context.getString(R.string.shopping_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.shopping_channel_description) },
        )
        val open = Intent(Intent.ACTION_VIEW, Uri.parse(DeepLinks.GROCERIES)).setPackage(context.packageName)
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(CoreUiR.drawable.ic_shopping_cart)
            .setContentTitle(context.getString(R.string.shopping_title, name ?: context.getString(R.string.shopping_someone)))
            .setContentText(context.getString(R.string.shopping_body))
            .setWhen(announcement.at.toEpochMilli())
            .setShowWhen(true)
            .setTimeoutAfter(announcement.kind.lasts.toMillis())
            .setAutoCancel(true)
            .setContentIntent(PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE))
            .build()
        NotificationManagerCompat.from(context).notify("shopping-${announcement.memberId}".hashCode(), notification)
    }

    private companion object {
        const val CHANNEL_ID = "shopping"
        const val KEY_SHOWN = "shown"
        const val TIMEOUT_MS = 30_000L
    }
}
