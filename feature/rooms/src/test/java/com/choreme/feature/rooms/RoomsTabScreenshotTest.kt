package com.choreme.feature.rooms

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
        task("bath1", "t1", "Clean bathroom", ChoreTemplate.CLEAN_BATHROOM, 10, 5),
        task("bath1", "t2", "Clean the toilet", ChoreTemplate.CLEAN_BATHROOM, 7, 9),
        task("bath1", "t3", "Wash the towels", ChoreTemplate.LAUNDRY, 7, 2),
        task("kitchen", "t4", "Clean kitchen", ChoreTemplate.CLEAN_KITCHEN, 2, 1),
        task("bed", "t5", "Change the sheets", ChoreTemplate.MAKE_BEDS, 14, 3),
        task("garden", "t6", "Water the plants", ChoreTemplate.WATER_PLANTS, 3, 4),
    )
    private val state = RoomsUiState(loading = false, rooms = roomOverviews(rooms, chores), canManage = true, today = today)

    private fun shoot(name: String, state: RoomsUiState, dark: Boolean, open: String? = null) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) { RoomsContent(state, RoomsActions(), openRoomId = open) }
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
    }

    @Test
    fun detailDark() = shoot("tab-rooms-detail-dark", state, dark = true, open = "bath1")

    /** Every kind of room's standard drawing, two by two, to check them by eye. */
    @Test
    fun sketches() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        RoomType.entries.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { RoomSketch(it, Modifier.weight(1f).aspectRatio(100f / 60f)) }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/room-sketches.png")
    }

    // Google Play screenshots: 1080 x 1920, in English and in Baianês.
    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun storeEn() {
        shoot("store-3-rooms-en", state, dark = false)
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun storePt() {
        shoot("store-3-rooms-pt", state, dark = false)
    }

    @Test
    @Config(qualifiers = "en-w360dp-h640dp-xxhdpi")
    fun storeDetailEn() {
        shoot("store-4-room-en", state, dark = false, open = "bath1")
    }

    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun storeDetailPt() {
        shoot("store-4-room-pt", state, dark = false, open = "bath1")
    }
}
