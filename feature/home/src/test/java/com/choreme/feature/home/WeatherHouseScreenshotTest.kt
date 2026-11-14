package com.choreme.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.weather.AirLevel
import com.choreme.core.model.weather.AirNote
import com.choreme.core.model.weather.DryingTip
import com.choreme.core.model.weather.PollenLevel
import com.choreme.core.model.weather.TripMode
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.adviceFor
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.LocalTime

/** The little house in every weather; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1800dp")
class WeatherHouseScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val noon = LocalDateTime.of(2026, 11, 16, 12, 0)
    private fun outside(code: Int, temp: Double, day: Boolean = true, rain: Int = 0, mm: Double = 0.0) =
        WeatherHour(noon, temp, temp - 3, rain, mm, windKmh = 14.0, gustKmh = 30.0, weatherCode = code, isDay = day)

    private fun weatherState(hour: WeatherHour, trips: List<MomentLine> = emptyList()) =
        HomeWeatherState(loading = false, now = hour, moments = trips)

    private val wetTrip = MomentLine(
        "Ramon", "Work", LocalTime.of(8, 0),
        adviceFor(listOf(outside(63, 6.0, rain = 80, mm = 1.4)), TripMode.BIKE),
    )
    private val dryTrip = MomentLine("Ana", "Gym", LocalTime.of(18, 30), adviceFor(listOf(outside(0, 11.0)), TripMode.WALK))

    private val skies = listOf(
        weatherState(outside(0, 21.0), listOf(dryTrip)),
        weatherState(outside(2, 14.0)),
        weatherState(outside(63, 8.0, rain = 80, mm = 1.4), listOf(wetTrip, dryTrip)),
        weatherState(outside(73, -2.0, rain = 70, mm = 1.0)),
        weatherState(outside(96, 17.0, rain = 90, mm = 7.0)),
        weatherState(outside(45, 5.0)),
        weatherState(outside(0, 6.0, day = false)),
        weatherState(outside(1, 19.0), listOf(dryTrip)).copy(air = AirNote(AirLevel.POOR, PollenLevel.HIGH), drying = DryingTip.HangToday),
    )

    /** The little house in every weather: sun, part cloud, rain with trips, snow, storm, fog and night. */
    @Test
    fun weather() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        skies.forEach { HomeWeatherCard(it, onClick = {}) }
                    }
                }
            }
        }
        compose.onAllNodesWithText("Gym", substring = true).assertCountEquals(3) // three cards have the gym trip
        compose.onNodeWithText("High pollen", substring = true).assertExists()
        compose.onNodeWithText("good day to dry", substring = true).assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/today-weather.png")
    }

    /** Dark mode: the picture keeps its own colours, only the card around it is dark. */
    @Test
    fun weatherDark() {
        compose.setContent {
            ChoreMeTheme(darkTheme = true, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp)) { HomeWeatherCard(skies[2], onClick = {}) }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/today-weather-dark.png")
    }

    /** What the dialog says: the day's trips with their weather, and the credit. */
    @Test
    fun weatherDialog() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                HomeWeatherDialog(skies[2], onDismiss = {})
            }
        }
        compose.onNodeWithText("Weather data by Open-Meteo.com", substring = true).assertExists()
    }
}
