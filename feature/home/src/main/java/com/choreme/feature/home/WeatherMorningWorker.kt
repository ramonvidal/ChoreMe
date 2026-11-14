package com.choreme.feature.home

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
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.LocationRepository
import com.choreme.core.data.PlanRepository
import com.choreme.core.location.RoutineLog
import com.choreme.core.model.AlertKind
import com.choreme.core.model.weather.activeEntries
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.weather.AirNote
import com.choreme.core.model.weather.Extreme
import com.choreme.core.model.weather.airNote
import com.choreme.core.model.weather.Moment
import com.choreme.core.model.weather.WeatherUnits
import com.choreme.core.model.weather.extremesIn
import com.choreme.core.settings.NotificationSettings
import com.choreme.core.settings.WeatherSettings
import com.choreme.core.ui.degrees
import com.choreme.core.ui.describe
import com.choreme.core.ui.label
import com.choreme.core.weather.WeatherRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Early every morning: the weather for the family's regular trips of the day, moment by moment ("8:00 Work: rain 80%,
 * rain jacket"), and a warning for anything extreme. Posted by each phone itself; says nothing on a day with no trips
 * and no storm. Needs the phone's own switch ("Weather in the morning") on.
 */
@HiltWorker
internal class WeatherMorningWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val plans: PlanRepository,
    private val locations: LocationRepository,
    private val weather: WeatherRepository,
    private val day: DayWeather,
    private val units: WeatherSettings,
    private val notifications: NotificationSettings,
    private val routine: RoutineLog,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!availability.isConfigured || !canNotify() || !notifications.isEnabled(AlertKind.WEATHER)) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val entries = withTimeoutOrNull(TIMEOUT_MS) { plans.observe(session.familyId).first() } ?: return Result.retry()
        val places = withTimeoutOrNull(TIMEOUT_MS) { locations.observePlaces(session.familyId).first() } ?: return Result.retry()
        val members = withTimeoutOrNull(TIMEOUT_MS) { families.observeMembers(session.familyId).first() }.orEmpty()
        val home = day.home(places) ?: return Result.success()

        val now = LocalDateTime.now()
        val forecast = weather.forecast(home) ?: return Result.retry()
        val extremes = extremesIn(forecast.between(now, now.plusHours(24)))
        val moments = day.moments(now.toLocalDate(), activeEntries(entries, members, now.toLocalDate(), session.memberId, routine.lastVisits()), places, home).filter { it.end.isAfter(now) }
        val air = airNote(weather.air(home)?.between(now, now.plusHours(12)).orEmpty())
        if (moments.isEmpty() && extremes.isEmpty() && air.isQuiet) return Result.success()

        val names = members.associate { it.id to it.name }
        show(air, forecast.at(now)?.let { units.units.value.degrees(it.temperatureC) + " · " + applicationContext.getString(it.sky.label) }, moments, extremes, names, session.memberId)
        return Result.success()
    }

    private fun canNotify() = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // Checked in canNotify().
    private fun show(air: AirNote, now: String?, moments: List<Moment>, extremes: Set<Extreme>, names: Map<String, String>, me: String) {
        val context = applicationContext
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.weather_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.weather_channel_description) },
        )
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val unit: WeatherUnits = units.units.value
        // Own trips first, then the others', each in the order of the day.
        val lines = buildList {
            extremes.forEach { add(context.getString(it.label)) }
            air.lines().forEach { add(context.getString(it)) }
            moments.sortedWith(compareBy({ it.entry.memberId != me }, { it.start })).forEach { moment ->
                val who = if (moment.entry.memberId == me) "" else " (${names[moment.entry.memberId].orEmpty()})"
                val advice = moment.advice.describe(context, unit).ifEmpty { context.getString(R.string.weather_all_good) }
                add("${moment.entry.start.format(time)} ${moment.entry.title}$who: $advice")
            }
        }
        val title = context.getString(R.string.weather_morning_title, now ?: context.getString(R.string.weather_title))
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = notifications.applyQuietHours(NotificationCompat.Builder(context, CHANNEL_ID))
            .setSmallIcon(R.drawable.ic_notification_home)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "weather-morning"
        const val NOTIFICATION_ID = 7
        const val TIMEOUT_MS = 30_000L
    }
}
