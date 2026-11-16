package com.choreme.feature.chores

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.chore.ChoreStatus
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomType
import com.choreme.core.model.room.newRoomTask
import com.choreme.core.model.room.roomOverviews
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The Rooms tab: the list of rooms and one room's page, light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1800dp")
class RoomsTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 4)

    private fun task(room: String, id: String, title: String, template: ChoreTemplate, every: Int, doneDaysAgo: Int?) =
        newRoomTask(room, title, template, every, doneDaysAgo?.let { today.minusDays(it.toLong()) }, today).copy(id = id)

    private val rooms = listOf(
        Room("bath1", "Bathroom 1", RoomType.BATHROOM, "Upstairs, next to the kids' room. The shower drain clogs easily."),
        Room("kitchen", "Kitchen", RoomType.KITCHEN),
        Room("bed", "Our bedroom", RoomType.BEDROOM),
        Room("kids", "Leo and Mia's room", RoomType.KIDS_ROOM),
        Room("garden", "Garden", RoomType.GARDEN),
    )
    private val chores: List<Chore> = listOf(
        // Leo took the bathroom; Mia says she did the toilet and waits for a parent's OK.
        task("bath1", "t1", "Clean bathroom", ChoreTemplate.CLEAN_BATHROOM, 10, 5).copy(status = ChoreStatus.CLAIMED, claimedBy = "leo"),
        task("bath1", "t2", "Clean the toilet", ChoreTemplate.CLEAN_BATHROOM, 7, 9).copy(status = ChoreStatus.PENDING_APPROVAL, claimedBy = "mia", completedBy = "mia"),
        task("bath1", "t3", "Wash the towels", ChoreTemplate.LAUNDRY, 7, 2),
        task("kitchen", "t4", "Clean kitchen", ChoreTemplate.CLEAN_KITCHEN, 2, 1),
        task("bed", "t5", "Change the sheets", ChoreTemplate.MAKE_BEDS, 14, 3),
        task("garden", "t6", "Water the plants", ChoreTemplate.WATER_PLANTS, 3, 4),
    )
    private val ramon = Member("ramon", "Ramon", Role.PARENT)
    private val leo = Member("leo", "Leo", Role.KID, color = 1)
    private val mia = Member("mia", "Mia", Role.KID, color = 2)
    private val members = listOf(ramon, leo, mia).associateBy { it.id }
    private val state = RoomsUiState(
        loading = false, rooms = roomOverviews(rooms, chores), canManage = true, today = today, me = ramon, members = members,
    )

    private fun shoot(name: String, state: RoomsUiState, dark: Boolean, open: String? = null) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) { Column { ChoresSubTabs(1, lateRooms = com.choreme.core.model.room.lateRoomCount(state.rooms, state.today)) {}; RoomsContent(state, RoomsActions(), openRoomId = open) } }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun list() {
        shoot("tab-rooms", state, dark = false)
        compose.onNodeWithText("Bathroom 1").assertExists()
        compose.onNodeWithText("Last done 2 days ago").assertExists()
    }

    @Test
    fun listDark() = shoot("tab-rooms-dark", state, dark = true)

    @Test
    fun empty() {
        shoot("tab-rooms-empty", RoomsUiState(loading = false, canManage = true, today = today), dark = false)
        compose.onNodeWithText("Your home, room by room").assertExists()
    }

    @Test
    fun detail() {
        shoot("tab-rooms-detail", state, dark = false, open = "bath1")
        compose.onNodeWithText("Clean the toilet").assertExists()
        // Who is on it, and what a parent can do: Leo took the bathroom, Mia's toilet waits to be approved.
        compose.onNodeWithText("Leo is on it").assertExists()
        compose.onNodeWithText("Mia says it's done").assertExists()
        compose.onNodeWithText("Approve").assertExists()
        compose.onNodeWithText("Me too").assertExists()
        compose.onAllNodesWithText("I'll do it").assertCountEquals(1) // only the towels are free to take
    }

    /** A kid sees the same tasks: can take or finish them, but not approve. */
    @Test
    fun detailKid() {
        shoot("tab-rooms-detail-kid", state.copy(me = leo, canManage = false), dark = false, open = "bath1")
        compose.onNodeWithText("You're on it").assertExists()
        compose.onAllNodesWithText("Approve").assertCountEquals(0)
    }

    @Test
    fun detailDark() = shoot("tab-rooms-detail-dark", state, dark = true, open = "bath1")

    // Google Play screenshots: 1080 x 1920, in English and in Baianês.
    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun storeEn() {
        shoot("store-4-rooms-en", state, dark = false)
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun storePt() {
        shoot("store-4-rooms-pt", state, dark = false)
    }
}
