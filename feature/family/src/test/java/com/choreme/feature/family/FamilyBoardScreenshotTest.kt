package com.choreme.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.FamilyMessage
import com.choreme.core.model.Member
import com.choreme.core.model.Note
import com.choreme.core.model.Role
import com.choreme.core.model.chore.ChoreDone
import com.choreme.core.model.chore.weeklySummary
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/** Notes, "this week" and an away label; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1200dp")
class FamilyBoardScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 26)
    private val mom = Member("mom", "Mom", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID, awayUntil = today.plusDays(4))
    private val mia = Member("mia", "Mia", Role.KID)

    @Test
    fun board() {
        val notes = listOf(
            Note("1", "Grandma arrives Friday at 18:00", "mom", Instant.parse("2026-10-26T09:00:00Z")),
            Note("2", "Wi-Fi: ChoreMe-Home / hunter2", "leo", Instant.parse("2026-10-20T09:00:00Z")),
        )
        val log = listOf(
            ChoreDone("1", "Wash the dishes", "mia", today, points = 2),
            ChoreDone("1", "Wash the dishes", "mia", today.minusDays(2), points = 2),
            ChoreDone("2", "Take out the trash", "leo", today.minusDays(1)),
        )
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        FamilyMessages(
                            messages = listOf(
                                FamilyMessage("m2", "Dinner's ready!", "mom", Instant.now().minusSeconds(120)),
                                FamilyMessage("m1", "I'll be 20 minutes late, start without me", "leo", Instant.now().minusSeconds(3600)),
                            ),
                            members = listOf(mom, leo, mia),
                            me = mom,
                            onSend = {},
                            onDelete = {},
                        )
                        NotesSection(notes, listOf(mom, leo, mia), me = mom, onAdd = {}, onEdit = { _, _ -> }, onDelete = {})
                        WeekSection(weeklySummary(log, listOf(mom, leo, mia), today))
                        awayLabel(leo, today)?.let { Text(it) }
                    }
                }
            }
        }
        compose.onNodeWithText("Grandma arrives Friday at 18:00").assertExists()
        compose.onNodeWithText("Dinner's ready!").assertExists()
        compose.onNodeWithText("4 points").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/family-board.png")
    }
}
