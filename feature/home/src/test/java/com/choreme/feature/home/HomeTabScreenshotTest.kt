package com.choreme.feature.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.home.DayTimeline
import com.choreme.core.model.home.EventEntry
import com.choreme.core.model.home.TripEntry
import com.choreme.core.model.weather.Moment
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.TripMode
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.adviceFor
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The whole Home tab: the weather house on top, the day's card and its timeline; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1800dp")
class HomeTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val monday = LocalDate.of(2026, 11, 16)
    private fun hour(h: Int, temp: Double, code: Int, rain: Int = 0, mm: Double = 0.0) =
        WeatherHour(monday.atTime(h, 0), temp, temp - 3, rain, mm, 14.0, 30.0, 70, 1.0, code)

    private fun trip(who: String, title: String, at: Int, mode: TripMode, hours: List<WeatherHour>) = TripEntry(
        LocalTime.of(at, 0),
        Moment(PlanEntry("t$at", who, title, setOf(DayOfWeek.MONDAY), LocalTime.of(at, 0), 40, mode), monday.atTime(at, 0), monday.atTime(at, 40), adviceFor(hours, mode)),
        who,
    )

    private val timeline = DayTimeline(
        date = monday,
        allDay = listOf("Grandma visits"),
        entries = listOf(
            trip("Ramon", "Work", 8, TripMode.BIKE, listOf(hour(8, 6.0, 63, 80, 1.4))),
            EventEntry(LocalTime.of(15, 30), "Dentist: Mia", "Dr. Weber"),
            trip("Ana", "Gym", 18, TripMode.WALK, listOf(hour(18, 11.0, 0))),
        ),
        choresToDo = 3,
        choresMine = 1,
        groceriesToBuy = 5,
    )

    private val state = HomeUiState(
        loading = false,
        date = monday,
        weather = HomeWeatherState(loading = false, now = hour(12, 8.0, 63, 80, 1.4), moments = emptyList()),
        timeline = timeline,
    )

    private fun shoot(name: String, state: HomeUiState, dark: Boolean) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) { HomeContent(state, onOpenChores = {}, onOpenGroceries = {}) }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun day() {
        shoot("tab-home", state, dark = false)
        compose.onNodeWithText("Today").assertExists()
        compose.onNodeWithText("Dentist: Mia").assertExists()
        compose.onNodeWithText("Grandma visits").assertExists()
    }

    @Test
    fun dayDark() = shoot("tab-home-dark", state, dark = true)

    @Test
    fun quietDay() {
        shoot("tab-home-quiet", state.copy(timeline = timeline.copy(allDay = emptyList(), entries = emptyList(), choresToDo = 0, choresMine = 0, groceriesToBuy = 0)), dark = false)
        compose.onNodeWithText("A quiet day").assertExists()
    }
}
