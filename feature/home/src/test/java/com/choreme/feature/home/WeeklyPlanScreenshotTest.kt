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
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.TripMode
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalTime

/** The weekly plan (the Week sub-tab of Home); saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1000dp")
class WeeklyPlanScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

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
                        WeeklyPlanContent(WeeklyPlanState(entries, places, listOf(ramon, ana), isParent = true, myMemberId = "ramon"), onSave = {}, onDelete = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Weekdays", substring = true).assertExists()
        compose.onNodeWithText("Add a trip").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/tab-home-week.png")
    }
}
