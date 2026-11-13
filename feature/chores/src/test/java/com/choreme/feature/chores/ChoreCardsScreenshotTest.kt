package com.choreme.feature.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.weather.TripMode
import com.choreme.core.model.weather.WeatherHour
import com.choreme.core.model.weather.adviceFor
import java.time.LocalDateTime
import java.time.LocalTime
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Today tab's cards, as a parent sees them; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1500dp")
class ChoreCardsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 25)
    private val mom = Member("mom", "Ramon", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID, color = 1)
    private val mia = Member("mia", "Mia", Role.KID, color = 2)
    private val state = ChoresUiState(loading = false, me = mom, today = today, members = mapOf("mom" to mom, "leo" to leo, "mia" to mia))

    private val dishes = Chore("1", "Wash the dishes", ChoreTemplate.DISHES, dueDate = today, reportedBy = "leo")
    private val laundry = Chore("2", "Laundry", ChoreTemplate.LAUNDRY, dueDate = today.minusDays(2), status = ChoreStatus.CLAIMED, claimedBy = "leo")
    private val bathroom = Chore("3", "Clean the bathroom", ChoreTemplate.CLEAN_BATHROOM, dueDate = today, status = ChoreStatus.PENDING_APPROVAL, completedBy = "leo")
    private val trash = Chore("4", "Take out the trash", ChoreTemplate.TRASH, dueDate = today, points = 2)
    private val hoover = Chore("6", "Hoover the living room", ChoreTemplate.VACUUM, dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "mom", helpers = setOf("mia"))
    private val garage = Chore("7", "Tidy the garage", ChoreTemplate.OTHER, dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "leo", helpers = setOf("mia"), points = 4)
    private val rent = Chore(
        "8", "Pay the rent", ChoreTemplate.OTHER, recurrence = com.choreme.core.model.chore.Recurrence.Monthly(5),
        dueDate = today, repeatUntil = LocalDate.of(2027, 12, 31),
    )
    private val leak = Chore("5", "Water leak under the sink", ChoreTemplate.CLEAN_KITCHEN, dueDate = today, reportedBy = "leo", urgent = true)

    @Test
    fun cards() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoresHeader(state.copy(chores = listOf(leak, dishes, laundry, hoover), board = buildTodayBoard(listOf(leak, dishes, laundry, hoover), today, "mom"), groceriesToBuy = 5), onOpenGroceries = {}, onAdd = {})
                        HomeWeatherCard(HomeWeatherState(loading = false, now = outside(2, 14.0)), onClick = {})
                        ChoreCard(trash, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {})
                        ChoreCard(hoover, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {}, mine = true)
                        ChoreCard(rent, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {})
                        ChoreCard(garage, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {})
                        ChoreCard(leak, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {})
                        AwaitingCard(bathroom, state, onApprove = {}, onReject = {})
                        ChoreCard(dishes, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {})
                        ChoreCard(laundry, state, onClaim = {}, onUnclaim = {}, onComplete = {}, onDoneBy = {}, onEdit = {}, onCalendar = {})
                        CompactRow(trash, trailing = "Tomorrow", onClick = {})
                        CompactRow(trash, trailing = "Leo", onClick = {}, done = true)
                    }
                }
            }
        }
        compose.onNodeWithText("Wash the dishes").assertExists()
        compose.onNodeWithText("Urgent").assertExists()
        compose.onNodeWithText("2 points").assertExists()
        // The hero card joins "1 is yours" and the grocery count in one line.
        compose.onNodeWithText("1 is yours", substring = true).assertExists()
        compose.onNodeWithText("You and Mia are on it").assertExists()
        compose.onNodeWithText("On it · 4 points").assertExists()
        compose.onNodeWithText("Every month on the 5th until 31 Dec 2027").assertExists()
        compose.onNodeWithContentDescription("Leo and Mia are on it").assertExists() // their avatars
        compose.onAllNodesWithText("Me too").assertCountEquals(2) // Laundry and the garage
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/today-cards.png")
    }

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
        compose.onNodeWithText("Gym", substring = true).assertExists()
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
