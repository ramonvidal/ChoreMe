package com.choreme.feature.agenda

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.agenda.AgendaDay
import com.choreme.core.model.agenda.CalendarEvent
import com.choreme.core.model.agenda.FamilyCalendar
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The whole Agenda tab, list and month, light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1600dp")
class AgendaTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 27)
    private val family = FamilyCalendar(1, "Family", "us@example.com", 0xFF2E6B52.toInt(), isWritable = true)
    private fun event(id: Long, title: String, day: LocalDate, hour: Int, location: String? = null) = CalendarEvent(
        id = id,
        calendarId = 1,
        title = title,
        start = day.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant(),
        end = day.atTime(hour + 1, 0).atZone(ZoneId.systemDefault()).toInstant(),
        allDay = false,
        location = location,
        color = family.color,
    )
    private val days = listOf(
        AgendaDay(today, listOf(event(1, "Swimming", today, 17, "Sports centre")), listOf(Chore("c1", "Vacuum", ChoreTemplate.VACUUM, dueDate = today))),
        AgendaDay(today.plusDays(1), listOf(event(2, "Dentist: Mia", today.plusDays(1), 9), event(3, "Parents' evening", today.plusDays(1), 19)), emptyList()),
        AgendaDay(today.plusDays(2), emptyList(), emptyList()),
        AgendaDay(today.plusDays(3), listOf(event(4, "Grandma visits", today.plusDays(3), 12)), listOf(Chore("c2", "Water the plants", ChoreTemplate.WATER_PLANTS, dueDate = today.plusDays(3)))),
    )
    private val state = AgendaUiState(
        hasPermission = true,
        calendars = listOf(family),
        selected = setOf(1L),
        today = today,
        days = days,
        month = YearMonth.from(today),
        monthDays = days.associateBy { it.date },
        selectedDay = today.plusDays(1),
    )

    private fun shoot(name: String, state: AgendaUiState, dark: Boolean) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) { AgendaContent(state, error = null, actions = AgendaActions()) }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun list() {
        shoot("tab-agenda-list", state, dark = false)
        compose.onNodeWithText("Swimming").assertExists()
    }

    @Test
    fun listDark() = shoot("tab-agenda-list-dark", state, dark = true)

    @Test
    fun month() {
        shoot("tab-agenda-month", state.copy(view = AgendaView.MONTH), dark = false)
        compose.onNodeWithText("Dentist: Mia").assertExists()
    }

    @Test
    fun monthDark() = shoot("tab-agenda-month-dark", state.copy(view = AgendaView.MONTH), dark = true)
}
