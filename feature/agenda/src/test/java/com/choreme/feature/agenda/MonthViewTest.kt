package com.choreme.feature.agenda

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.agenda.AgendaDay
import com.choreme.core.model.chore.Chore
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en")
class MonthViewTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 25)
    private val vacuum = Chore(id = "1", title = "Vacuum", dueDate = today.plusDays(2))
    private val state = AgendaUiState(
        hasPermission = true,
        today = today,
        view = AgendaView.MONTH,
        month = YearMonth.of(2026, 10),
        selectedDay = today,
        monthDays = mapOf(today.plusDays(2) to AgendaDay(today.plusDays(2), emptyList(), listOf(vacuum))),
    )

    @Test
    fun `shows the month and tells which day was tapped`() {
        var tapped: LocalDate? = null
        compose.setContent { MonthView(state, onMonth = {}, onSelect = { tapped = it }, onOpenEvent = {}) }

        compose.onNodeWithText("October 2026").assertExists()
        compose.onNodeWithText("15").performClick()
        assertEquals(LocalDate.of(2026, 10, 15), tapped)
    }

    @Test
    fun `lists the selected day, or says it's empty`() {
        compose.setContent { MonthView(state.copy(selectedDay = today.plusDays(2)), onMonth = {}, onSelect = {}, onOpenEvent = {}) }
        compose.onNodeWithText("Vacuum").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/agenda-month.png")
    }

    @Test
    fun `an empty day says so`() {
        compose.setContent { MonthView(state, onMonth = {}, onSelect = {}, onOpenEvent = {}) }
        compose.onNodeWithText("Nothing on this day").assertExists()
    }

    @Test
    fun `arrows move between months`() {
        var shown: YearMonth? = null
        compose.setContent { MonthView(state, onMonth = { shown = it }, onSelect = {}, onOpenEvent = {}) }
        compose.onNodeWithContentDescriptionCompat("Next month").performClick()
        assertEquals(YearMonth.of(2026, 11), shown)
    }
}

private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onNodeWithContentDescriptionCompat(label: String) =
    onNode(androidx.compose.ui.test.hasContentDescription(label))
