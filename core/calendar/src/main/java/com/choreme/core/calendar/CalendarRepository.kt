package com.choreme.core.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.agenda.FamilyCalendar
import com.choreme.core.model.agenda.NewEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads and writes the calendars the phone already syncs, such as a shared Google Calendar.
 * Google's own sync then carries new events to everyone sharing the calendar, and sends the
 * alerts. No Google Cloud setup is needed, only the calendar permission.
 */
@Singleton
class CalendarRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val resolver get() = context.contentResolver

    fun hasPermission(): Boolean = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun observeCalendars(): Flow<List<FamilyCalendar>> = changes().map { queryCalendars() }.flowOn(Dispatchers.IO)

    /** Events in [calendarIds] between [from] and [until], with repeating events expanded. */
    fun observeEvents(calendarIds: Set<Long>, from: Instant, until: Instant): Flow<List<CalendarEvent>> =
        changes().map { queryEvents(calendarIds, from, until) }.flowOn(Dispatchers.IO)

    /** Adds the event, with an alert if asked for. Returns the new event's id. */
    suspend fun add(calendarId: Long, event: NewEvent, zone: ZoneId = ZoneId.systemDefault()): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DTSTART, event.start(zone).toEpochMilli())
            put(CalendarContract.Events.DTEND, event.end(zone).toEpochMilli())
            put(CalendarContract.Events.ALL_DAY, if (event.allDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, if (event.allDay) ZoneOffset.UTC.id else zone.id)
            put(CalendarContract.Events.HAS_ALARM, if (event.reminderMinutes(zone) != null) 1 else 0)
        }
        val eventId = checkNotNull(resolver.insert(CalendarContract.Events.CONTENT_URI, values)) { "Calendar refused the event" }
            .let(ContentUris::parseId)
        event.reminderMinutes(zone)?.let { minutes ->
            resolver.insert(
                CalendarContract.Reminders.CONTENT_URI,
                ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, minutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                },
            )
        }
        eventId
    }

    /** Opens the event in the phone's calendar app, for editing or details. */
    fun openIntent(event: CalendarEvent): Intent =
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.id))
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.start.toEpochMilli())
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.end.toEpochMilli())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Emits once at the start and again whenever the phone's calendar data changes. */
    private fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        if (hasPermission()) resolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate()

    private fun queryCalendars(): List<FamilyCalendar> {
        if (!hasPermission()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
        )
        val calendars = mutableListOf<FamilyCalendar>()
        resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, "${CalendarContract.Calendars.VISIBLE} = 1", null, null)
            ?.use { cursor ->
                while (cursor.moveToNext()) {
                    calendars += FamilyCalendar(
                        id = cursor.getLong(0),
                        name = cursor.getString(1).orEmpty(),
                        account = cursor.getString(2).orEmpty(),
                        color = cursor.getInt(3),
                        isWritable = cursor.getInt(4) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR,
                    )
                }
            }
        return calendars.sortedWith(compareBy({ it.account }, { it.name.lowercase() }))
    }

    private fun queryEvents(calendarIds: Set<Long>, from: Instant, until: Instant): List<CalendarEvent> {
        if (!hasPermission() || calendarIds.isEmpty()) return emptyList()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.DISPLAY_COLOR,
        )
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .also {
                ContentUris.appendId(it, from.toEpochMilli())
                ContentUris.appendId(it, until.toEpochMilli())
            }
            .build()
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN (${calendarIds.joinToString()})"
        val events = mutableListOf<CalendarEvent>()
        resolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
            while (cursor.moveToNext()) {
                events += CalendarEvent(
                    id = cursor.getLong(0),
                    calendarId = cursor.getLong(1),
                    title = cursor.getString(2).orEmpty(),
                    start = Instant.ofEpochMilli(cursor.getLong(3)),
                    end = Instant.ofEpochMilli(cursor.getLong(4)),
                    allDay = cursor.getInt(5) == 1,
                    location = cursor.getString(6)?.takeIf { it.isNotBlank() },
                    color = cursor.getInt(7),
                )
            }
        }
        return events
    }

    companion object {
        val PERMISSIONS = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
    }
}
