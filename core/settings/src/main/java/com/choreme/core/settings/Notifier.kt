package com.choreme.core.settings

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.choreme.core.model.AlertKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** A kind of notification as Android's settings list it: its id, its name and how loud it is. */
class AlertChannel(
    val id: String,
    @StringRes val name: Int,
    @StringRes val description: Int? = null,
    val importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
)

/**
 * Shows the app's notifications, all the same way: only when Android allows it, on their channel (made on first use),
 * and without sound during this phone's quiet hours. Each feature only says what the notification says.
 */
@Singleton
class Notifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: NotificationSettings,
) {
    /** Whether Android lets the app show notifications (people are asked since Android 13). */
    fun canNotify(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Whether a notification of [kind] would show: Android allows it and the person didn't turn that kind off. */
    fun allows(kind: AlertKind): Boolean = canNotify() && settings.isEnabled(kind)

    /** Shows notification [id] on [channel], filled in by [content]. Does nothing when Android doesn't allow notifications. */
    @SuppressLint("MissingPermission") // Checked by canNotify().
    fun show(channel: AlertChannel, id: Int, content: NotificationCompat.Builder.() -> Unit) {
        if (!canNotify()) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(channel.id, context.getString(channel.name), channel.importance)
                .apply { channel.description?.let { description = context.getString(it) } },
        )
        val builder = settings.applyQuietHours(NotificationCompat.Builder(context, channel.id)).apply(content)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /** Tapping opens this address in the app, e.g. one of the deep links in `DeepLinks`. */
    fun open(address: String, requestCode: Int = 0): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(address)).setPackage(context.packageName)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    }
}
