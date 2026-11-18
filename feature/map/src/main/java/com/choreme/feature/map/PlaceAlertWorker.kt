package com.choreme.feature.map

import com.choreme.core.settings.AlertChannel
import com.choreme.core.settings.Notifier
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import com.choreme.core.settings.NotificationSettings
import com.choreme.core.model.AlertKind
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.LocationRepository
import com.choreme.core.model.location.PlaceEvent
import com.choreme.core.model.location.PlaceEventType
import com.choreme.core.model.session.SessionState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shows other members' arrivals and departures since the last check, and tidies up old ones of our own. */
@HiltWorker
internal class PlaceAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val locations: LocationRepository,
    private val store: PresenceStore,
    private val notifications: NotificationSettings,
    private val notifier: Notifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!availability.isConfigured) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()

        val since = store.alertsCheckedUntil
        val now = Instant.now()
        if (since == null) {
            // First run on this phone: start from now rather than replaying history.
            store.alertsCheckedUntil = now
            return Result.success()
        }
        return try {
            // Events carry the time of the move, but the other phone may upload them up to ~15
            // minutes later; looking back further and skipping shown ones catches those too.
            val shown = store.shownEventIds
            val recent = locations.eventsSince(session.familyId, since.minus(LOOK_BACK))
            val events = recent.filter { it.memberId != session.memberId && it.id !in shown }
            if (notifier.allows(AlertKind.ARRIVALS) && events.isNotEmpty()) {
                val names = families.observeMembers(session.familyId).first().associate { it.id to it.name }
                events.forEach { show(it, names[it.memberId]) }
            }
            // Anything older than this window won't come back in the next check.
            store.shownEventIds = recent.map { it.id }.toSet()
            store.alertsCheckedUntil = now
            locations.deleteOwnEventsBefore(session.familyId, session.memberId, now.minus(KEEP_EVENTS))
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun show(event: PlaceEvent, name: String?) {
        val context = applicationContext
        val who = name ?: context.getString(R.string.someone)
        val title = context.getString(
            if (event.type == PlaceEventType.ARRIVED) R.string.alert_arrived else R.string.alert_left,
            who,
            event.placeName,
        )
        val time = event.at.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        // One notification per member, so a new event replaces their older one.
        notifier.show(CHANNEL, event.memberId.hashCode()) {
            setSmallIcon(R.drawable.ic_notification_place)
            setContentTitle(title)
            setContentText(time)
            setWhen(event.at.toEpochMilli())
            setShowWhen(true)
            setGroup(GROUP)
            setAutoCancel(true)
            setContentIntent(launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE) })
        }
    }

    private companion object {
        val CHANNEL = AlertChannel(CHANNEL_ID, R.string.alerts_channel, R.string.alerts_channel_description, NotificationManager.IMPORTANCE_DEFAULT)
        const val CHANNEL_ID = "place-alerts"
        const val GROUP = "place-alerts"
        const val TIMEOUT_MS = 30_000L
        val KEEP_EVENTS: Duration = Duration.ofDays(2)
        val LOOK_BACK: Duration = Duration.ofHours(1)
    }
}
