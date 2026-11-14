package com.choreme.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.Place
import com.choreme.core.model.weather.DayOutlook
import com.choreme.core.model.weather.PauseReason
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.RoutineSuggestion
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.dayOutlook
import com.choreme.core.model.weather.momentsOn
import com.choreme.core.model.weather.TripMode
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The weekly plan (the Week sub-tab of Home); saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1000dp")
class WeeklyPlanScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun outlook(entries: List<PlanEntry>): List<DayOutlook> {
        val monday = LocalDate.of(2026, 11, 16)
        fun hour(date: LocalDate, h: Int, code: Int, temp: Double, rain: Int = 0, mm: Double = 0.0) =
            WeatherHour(date.atTime(h, 0), temp, rainProbability = rain, rainMm = mm, weatherCode = code)
        val skies = listOf(Triple(63, 6.0, 80), Triple(3, 8.0, 10), Triple(0, 12.0, 0), Triple(2, 11.0, 5))
        return skies.mapIndexedNotNull { i, (code, temp, rain) ->
            val date = monday.plusDays(i.toLong())
            val hours = (6..22).map { hour(date, it, code, temp, rain, if (rain > 50) 0.4 else 0.0) }
            val moments = momentsOn(date, entries) { entry, from, to ->
                hours.filter { !it.time.isBefore(from.withMinute(0)) && !it.time.isAfter(to) }
            }
            dayOutlook(date, hours, moments)
        }
    }

    @Test
    fun weeklyPlan() {
        val places = listOf(
            Place("home", "Home", GeoPoint(52.5, 13.4), 100),
            Place("work", "Work", GeoPoint(52.52, 13.45), 100),
            Place("gym", "Gym", GeoPoint(52.51, 13.42), 100),
        )
        val ramon = Member("ramon", "Ramon", Role.PARENT)
        val ana = Member("ana", "Ana", Role.PARENT)
        val entries = listOf(
            PlanEntry("a", "ramon", "Work", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), LocalTime.of(8, 0), 40, TripMode.BIKE, "work"),
            PlanEntry("b", "ana", "Gym", setOf(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY), LocalTime.of(18, 30), 50, TripMode.WALK, "gym"),
            PlanEntry("c", "ramon", "Gym", setOf(DayOfWeek.SATURDAY), LocalTime.of(10, 0), 30, TripMode.CAR, "gym"),
        )
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(16.dp)) {
                        WeeklyPlanContent(WeeklyPlanState(entries, places, listOf(ramon, ana), isParent = true, myMemberId = "ramon", outlook = outlook(entries), suggestions = listOf(RoutineSuggestion("gym", setOf(DayOfWeek.WEDNESDAY), LocalTime.of(18, 0), 4)), paused = mapOf("b" to PauseReason.AWAY, "c" to PauseReason.UNSEEN)), onSave = {}, onDelete = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Weekdays", substring = true).assertExists()
        compose.onNodeWithText("Add a trip").assertExists()
        compose.onNodeWithText("The week ahead").assertExists()
        compose.onNodeWithText("We noticed a habit").assertExists()
        compose.onNodeWithText("Paused while they are away").assertExists()
        compose.onNodeWithText("Paused: not seen", substring = true).assertExists()
        compose.onNodeWithText("Add to the plan").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/tab-home-week.png")
    }
}
