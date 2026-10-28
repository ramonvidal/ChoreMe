package com.choreme.feature.family

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Family
import com.choreme.core.model.FamilyMessage
import com.choreme.core.model.Member
import com.choreme.core.model.Note
import com.choreme.core.model.Role
import com.choreme.core.model.chore.ChoreDone
import com.choreme.core.model.chore.weeklySummary
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

/** The whole Family tab as a parent sees it, light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h2000dp")
class FamilyTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.now()
    private val mom = Member("mom", "Ramon", Role.PARENT, color = 0)
    private val ana = Member("ana", "Ana", Role.PARENT, color = 3)
    private val leo = Member("leo", "Leo", Role.KID, color = 1, awayUntil = today.plusDays(4))
    private val mia = Member("mia", "Mia", Role.KID, color = 2)
    private val members = listOf(mom, ana, leo, mia)
    private val log = listOf(
        ChoreDone("1", "Wash the dishes", "mia", today, points = 2),
        ChoreDone("2", "Tidy the garage", "leo", today.minusDays(1), points = 4, helpers = setOf("mia")),
        ChoreDone("3", "Laundry", "ana", today.minusDays(2), points = 3),
        ChoreDone("4", "Take out the trash", "mom", today),
    )
    private val state = FamilyUiState(
        me = SessionState.InFamily("f", "mom", Role.PARENT),
        family = Family("f", "The Vidals", "ABC234"),
        members = members,
        notes = listOf(
            Note("1", "Grandma arrives Friday at 18:00", "ana", Instant.now().minusSeconds(7200)),
            Note("2", "Wi-Fi: ChoreMe-Home / hunter2", "mom", Instant.now().minusSeconds(86_400)),
        ),
        messages = listOf(
            FamilyMessage("m2", "Dinner's ready!", "ana", Instant.now().minusSeconds(120)),
            FamilyMessage("m1", "I'll be 20 minutes late, start without me", "mom", Instant.now().minusSeconds(3600)),
        ),
        week = weeklySummary(log, members, today),
        today = today,
    )

    private fun shoot(name: String, dark: Boolean, fontScale: Float = 1f) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                Surface(Modifier.fillMaxSize()) {
                    FamilyContent(state, ActionState(), signOutNeedsApprovalAgain = false, actions = FamilyActions()) {
                        Text("(This phone's settings)")
                    }
                }
            }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun parent() {
        shoot("tab-family", dark = false)
        compose.onNodeWithText("Dinner's ready!").assertExists()
    }

    @Test
    fun parentDark() = shoot("tab-family-dark", dark = true)

    /** The phone's text size turned up to the largest step: nothing should be cut off. */
    @Test
    fun largeText() {
        shoot("tab-family-large-text", dark = false, fontScale = 1.5f)
    }
}
