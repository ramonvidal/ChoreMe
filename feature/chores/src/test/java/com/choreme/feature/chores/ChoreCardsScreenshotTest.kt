package com.choreme.feature.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.buildTodayBoard
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
    private val trash = Chore("4", "Take out the trash", ChoreTemplate.TRASH, dueDate = today)
    private val leak = Chore("5", "Water leak under the sink", ChoreTemplate.CLEAN_KITCHEN, dueDate = today, reportedBy = "leo", urgent = true)

    @Test
    fun cards() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChoresHeader(state.copy(chores = listOf(leak, dishes, laundry), board = buildTodayBoard(listOf(leak, dishes, laundry), today), groceriesToBuy = 5), onOpenGroceries = {})
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
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/today-cards.png")
    }
}
