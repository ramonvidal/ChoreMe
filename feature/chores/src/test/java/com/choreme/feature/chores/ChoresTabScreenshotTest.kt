package com.choreme.feature.chores

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreDone
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.chore.familyPoints
import com.choreme.core.model.chore.weeklySummary
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The whole Chores tab, for a parent and a kid, light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1800dp")
class ChoresTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 28)
    private val mom = Member("mom", "Ramon", Role.PARENT, color = 0)
    private val ana = Member("ana", "Ana", Role.PARENT, color = 3)
    private val leo = Member("leo", "Leo", Role.KID, color = 1)
    private val mia = Member("mia", "Mia", Role.KID, color = 2)
    private val members = listOf(mom, ana, leo, mia)

    private val chores = listOf(
        Chore("1", "Water leak under the sink", ChoreTemplate.CLEAN_KITCHEN, dueDate = today, reportedBy = "leo", urgent = true),
        Chore("2", "Hoover the living room", ChoreTemplate.VACUUM, dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "mom", points = 2),
        Chore("3", "Tidy the garage", ChoreTemplate.OTHER, dueDate = today, status = ChoreStatus.CLAIMED, claimedBy = "leo", helpers = setOf("mia"), points = 4),
        Chore("4", "Wash the dishes", ChoreTemplate.DISHES, recurrence = Recurrence.EveryDays(1), dueDate = today, reportedBy = "ana"),
        Chore("5", "Laundry", ChoreTemplate.LAUNDRY, dueDate = today.minusDays(2), status = ChoreStatus.CLAIMED, claimedBy = "ana"),
        Chore("6", "Clean the bathroom", ChoreTemplate.CLEAN_BATHROOM, dueDate = today, status = ChoreStatus.PENDING_APPROVAL, claimedBy = "mia", completedBy = "mia", points = 3),
        Chore("7", "Take out the trash", ChoreTemplate.TRASH, recurrence = Recurrence.EveryDays(2), dueDate = today.plusDays(1)),
        Chore("8", "Water the plants", ChoreTemplate.WATER_PLANTS, dueDate = today.plusDays(3)),
        Chore("9", "Make the beds", ChoreTemplate.MAKE_BEDS, dueDate = today, status = ChoreStatus.DONE, lastDoneBy = "leo", lastDoneOn = today),
    )
    private val log = listOf(
        ChoreDone("9", "Make the beds", "leo", today),
        ChoreDone("10", "Cook dinner", "ana", today.minusDays(1), points = 3),
        ChoreDone("11", "Mow the lawn", "mom", today.minusDays(2), points = 5),
        ChoreDone("12", "Fold clothes", "mia", today.minusDays(3), points = 2, helpers = setOf("leo")),
    )

    private fun state(me: Member) = ChoresUiState(
        loading = false,
        familyId = "f",
        me = me,
        today = today,
        board = buildTodayBoard(chores, today, me.id),
        chores = chores,
        members = members.associateBy { it.id },
        groceriesToBuy = 5,
        week = weeklySummary(log, members, today),
        weekPoints = familyPoints(log, today),
    )

    private fun shoot(name: String, me: Member, dark: Boolean) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    ChoresContent(state(me), reminders = emptyMap(), onOpenGroceries = {}, actions = ChoresActions())
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun parent() {
        shoot("tab-chores", mom, dark = false)
        compose.onNodeWithText("Hoover the living room").assertExists()
    }

    @Test
    fun parentDark() = shoot("tab-chores-dark", mom, dark = true)

    @Test
    fun kid() = shoot("tab-chores-kid", mia, dark = false)
}
