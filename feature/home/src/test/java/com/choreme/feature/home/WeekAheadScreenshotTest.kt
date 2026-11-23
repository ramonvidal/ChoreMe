package com.choreme.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.weather.DayOutlook
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.TripMode
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.dayOutlook
import com.choreme.core.model.weather.momentsOn
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The Week sub-tab of Home: the week ahead with the trips of each day; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1000dp")
class WeekAheadScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val ramon = Member("ramon", "Ramon", Role.PARENT)
    private val ana = Member("ana", "Ana", Role.PARENT)
    private val entries = listOf(
        PlanEntry("a", "ramon", "Work", setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY), LocalTime.of(8, 0), 40, TripMode.BIKE, "work"),
        PlanEntry("b", "ana", "Gym", setOf(DayOfWeek.TUESDAY), LocalTime.of(18, 30), 50, TripMode.WALK, "gym"),
    )

    /** The same trips named in Portuguese, for the Baianês store pictures. */
    private val portugueseEntries = entries.map { it.copy(title = mapOf("Work" to "Trabalho", "Gym" to "Academia")[it.title] ?: it.title) }

    private fun outlook(entries: List<PlanEntry> = this.entries): List<DayOutlook> {
        val monday = LocalDate.of(2026, 11, 16)
        fun hour(date: LocalDate, h: Int, code: Int, temp: Double, rain: Int, wind: Double) =
            WeatherHour(date.atTime(h, 0), temp, rainProbability = rain, rainMm = if (rain > 50) 0.4 else 0.0, windKmh = wind, humidity = 55, weatherCode = code)
        val skies = listOf(Triple(63, 6.0, 80), Triple(3, 8.0, 10), Triple(0, 18.0, 0), Triple(2, 15.0, 5))
        return skies.mapIndexedNotNull { i, (code, temp, rain) ->
            val date = monday.plusDays(i.toLong())
            val hours = (6..22).map { hour(date, it, code, temp, rain, 10.0) }
            val moments = momentsOn(date, entries) { _, from, to ->
                hours.filter { !it.time.isBefore(from.withMinute(0)) && !it.time.isAfter(to) }
            }
            dayOutlook(date, hours, moments)
        }
    }

    private val state get() = WeekAheadState(outlook(), members = listOf(ramon, ana), myMemberId = "ramon", hasTrips = true, loading = false)

    private fun shootStore(name: String, pt: Boolean = false) {
        val state = if (pt) state.copy(outlook = outlook(portugueseEntries)) else state
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    Column {
                        HomeSubTabs(1) {}
                        WeekAheadContent(state, onPlanTrips = {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    // Google Play screenshots: 1080 x 1920, in English and in Baianês.
    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun storeEn() = shootStore("store-6-week-en")

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun storePt() = shootStore("store-6-week-pt", pt = true)

    @Test
    fun weekAhead() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    Column {
                        HomeSubTabs(1) {}
                        WeekAheadContent(state, onPlanTrips = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Plan trips in Settings").assertExists()
        compose.onNodeWithText("Trips are planned under Settings.").assertExists()
        compose.onAllNodesWithText("good for drying", substring = true).assertCountEquals(2) // the 3rd and 4th days
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/tab-home-week.png")
    }
}
