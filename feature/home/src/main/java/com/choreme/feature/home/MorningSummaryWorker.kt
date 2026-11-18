package com.choreme.feature.home

import com.choreme.core.settings.AlertChannel
import com.choreme.core.settings.Notifier
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.choreme.core.calendar.CalendarChoice
import com.choreme.core.calendar.CalendarRepository
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.FirebaseAvailability
import com.choreme.core.data.GroceryRepository
import com.choreme.core.data.LocationRepository
import com.choreme.core.data.PlanRepository
import com.choreme.core.location.RoutineLog
import com.choreme.core.model.AlertKind
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.chore.RoomFilter
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.chore.filteredBy
import com.choreme.core.model.chore.reminderFor
import com.choreme.core.model.grocery.groceriesToBuy
import com.choreme.core.model.home.EventEntry
import com.choreme.core.model.home.TripEntry
import com.choreme.core.model.home.dayTimeline
import com.choreme.core.model.session.SessionState
import com.choreme.core.model.weather.AirNote
import com.choreme.core.model.weather.Extreme
import com.choreme.core.model.weather.Moment
import com.choreme.core.model.weather.activeEntries
import com.choreme.core.model.weather.airNote
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Early every morning, one notification for the whole day: the weather outside, storm and air warnings, then the day in
 * order (calendar events and the family's regular trips with their weather), and what there is to do (chores, room
 * tasks, groceries). Posted by each phone itself. The two switches of "This phone" decide which parts it holds: the
 * weather and trips, and the chores; the calendar comes along with either. Says nothing on a day with nothing to say.
 */
@HiltWorker
internal class MorningSummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val availability: FirebaseAvailability,
    private val families: FamilyRepository,
    private val plans: PlanRepository,
    private val locations: LocationRepository,
    private val chores: ChoreRepository,
    private val groceries: GroceryRepository,
    private val calendar: CalendarRepository,
    private val choice: CalendarChoice,
    private val weather: WeatherRepository,
    private val day: DayWeather,
    private val units: WeatherSettings,
    private val notifications: NotificationSettings,
    private val notifier: Notifier,
    private val routine: RoutineLog,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!availability.isConfigured || !notifier.canNotify()) return Result.success()
        val weatherOn = notifications.isEnabled(AlertKind.WEATHER)
        val choresOn = notifications.isEnabled(AlertKind.MORNING_SUMMARY)
        if (!weatherOn && !choresOn) return Result.success()
        val session = withTimeoutOrNull(TIMEOUT_MS) { families.session.first { it != SessionState.Loading } }
            as? SessionState.InFamily ?: return Result.success()
        val familyId = session.familyId
        val entries = withTimeoutOrNull(TIMEOUT_MS) { plans.observe(familyId).first() } ?: return Result.retry()
        val places = withTimeoutOrNull(TIMEOUT_MS) { locations.observePlaces(familyId).first() } ?: return Result.retry()
        val members = withTimeoutOrNull(TIMEOUT_MS) { families.observeMembers(familyId).first() }.orEmpty()
        val allChores = withTimeoutOrNull(TIMEOUT_MS) { chores.observeChores(familyId).first() }.orEmpty()
        val groceryItems = withTimeoutOrNull(TIMEOUT_MS) { groceries.observeItems(familyId).first() }.orEmpty()

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val zone = ZoneId.systemDefault()
        val away = members.firstOrNull { it.id == session.memberId }?.isAway(today) == true

        // The weather and the trips (left out when that switch is off).
        var nowText: String? = null
        var extremes = emptySet<Extreme>()
        var air = AirNote()
        var moments = emptyList<Moment>()
        if (weatherOn) {
            val home = day.home(places)
            val forecast = home?.let { weather.forecast(it) }
            if (home != null && forecast != null) {
                nowText = forecast.at(now)?.let { units.units.value.degrees(it.temperatureC) + " · " + applicationContext.getString(it.sky.label) }
                extremes = extremesIn(forecast.between(now, now.plusHours(24)))
                air = airNote(weather.air(home)?.between(now, now.plusHours(12)).orEmpty())
                val active = activeEntries(entries, members, today, session.memberId, routine.lastVisits())
                moments = day.moments(today, active, places, home).filter { it.end.isAfter(now) }
            }
        }

        val houseBoard = buildTodayBoard(allChores.filteredBy(RoomFilter.NO_ROOM), today, session.memberId)
        val roomBoard = buildTodayBoard(allChores.filteredBy(RoomFilter.ROOMS), today, session.memberId)
        val names = members.associate { it.id to it.name }
        val timeline = dayTimeline(today, eventsOfToday(zone), moments, names, houseBoard, groceryItems.groceriesToBuy(), zone, roomBoard)

        val context = applicationContext
        val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        val unit = units.units.value
        val lines = buildList {
            extremes.forEach { add(context.getString(it.label)) }
            air.lines().forEach { add(context.getString(it)) }
            if (timeline.allDay.isNotEmpty()) add(context.getString(R.string.morning_all_day, timeline.allDay.joinToString(", ")))
            // The day in order: own trips first is lost here on purpose, the order of the day matters more.
            timeline.entries.forEach { entry ->
                when (entry) {
                    is EventEntry -> add("${entry.time.format(time)} ${entry.title}" + (entry.place?.let { " · $it" } ?: ""))
                    is TripEntry -> {
                        val moment = entry.moment
                        val who = if (moment.entry.memberId == session.memberId) "" else " (${entry.who})"
                        val advice = moment.advice.describe(context, unit).ifEmpty { context.getString(R.string.weather_all_good) }
                        add("${entry.time.format(time)} ${moment.entry.title}$who: $advice")
                    }
                }
            }
            if (choresOn && !away) choresLine(houseBoard, roomBoard.mine.size, session.role, today)?.let { add(it) }
            if (choresOn && timeline.groceriesToBuy > 0) {
                add(context.resources.getQuantityString(R.plurals.home_groceries, timeline.groceriesToBuy, timeline.groceriesToBuy))
            }
        }
        if (lines.isEmpty()) return Result.success()
        show(nowText, lines)
        return Result.success()
    }

    private suspend fun eventsOfToday(zone: ZoneId): List<CalendarEvent> {
        if (!calendar.hasPermission()) return emptyList()
        val selected = choice.selected.first()?.takeIf { it.isNotEmpty() } ?: return emptyList()
        val from = LocalDate.now().atStartOfDay(zone).toInstant()
        return withTimeoutOrNull(TIMEOUT_MS) { calendar.observeEvents(selected, from, from.plusSeconds(24 * 3600)).first() }.orEmpty()
    }

    private fun choresLine(
        board: com.choreme.core.model.chore.TodayBoard,
        roomTasks: Int,
        role: com.choreme.core.model.Role,
        today: LocalDate,
    ): String? {
        val res = applicationContext.resources
        val reminder = reminderFor(board, role, today)
        val parts = buildList {
            reminder?.let {
                if (it.mine > 0) add(res.getQuantityString(R.plurals.morning_mine, it.mine, it.mine))
                if (it.toDo > 0) add(res.getQuantityString(R.plurals.home_chores, it.toDo, it.toDo))
                if (it.overdue > 0) add(res.getQuantityString(R.plurals.morning_overdue, it.overdue, it.overdue))
                if (it.awaitingApproval > 0) add(res.getQuantityString(R.plurals.morning_awaiting, it.awaitingApproval, it.awaitingApproval))
            }
            if (roomTasks > 0) add(res.getQuantityString(R.plurals.home_rooms, roomTasks, roomTasks))
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")?.let { applicationContext.getString(R.string.morning_chores_line, it) }
    }

    private fun show(now: String?, lines: List<String>) {
        val context = applicationContext
        val title = now?.let { context.getString(R.string.morning_title, it) } ?: context.getString(R.string.morning_title_plain)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val contentIntent = launch?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        notifier.show(CHANNEL, NOTIFICATION_ID) {
            setSmallIcon(R.drawable.ic_notification_home)
            setContentTitle(title)
            setContentText(lines.first())
            setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            setContentIntent(contentIntent)
            setAutoCancel(true)
        }
    }

    private companion object {
        val CHANNEL = AlertChannel(CHANNEL_ID, R.string.morning_channel, R.string.morning_channel_description, NotificationManager.IMPORTANCE_DEFAULT)
        const val CHANNEL_ID = "morning-summary"
        const val NOTIFICATION_ID = 7
        const val TIMEOUT_MS = 30_000L
    }
}
