package com.choreme.feature.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.location.GeoPoint
import com.choreme.core.model.location.LocationStatus
import com.choreme.core.model.location.MemberLocation
import com.choreme.core.model.location.Place
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.Instant

/** The whole Map tab (with a stand-in for the map tiles), light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1400dp")
class MapTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val home = Place("h", "Home", GeoPoint(41.39, 2.17), 100)
    private val school = Place("s", "School", GeoPoint(41.40, 2.16), 250)
    private val mom = Member("mom", "Ramon", Role.PARENT, color = 0)
    private fun known(id: String, place: Place?, minutes: Long) = LocationStatus.Known(
        MemberLocation(id, GeoPoint(41.39, 2.17), 20f, Instant.now().minusSeconds(minutes * 60)),
        place,
        Duration.ofMinutes(minutes),
    )
    private val state = MapUiState(
        loading = false,
        me = mom,
        members = listOf(
            MemberOnMap(mom, known("mom", home, 2)),
            MemberOnMap(Member("ana", "Ana", Role.PARENT, color = 3), known("ana", null, 15)),
            MemberOnMap(Member("leo", "Leo", Role.KID, color = 1), known("leo", school, 90)),
            MemberOnMap(Member("mia", "Mia", Role.KID, color = 2), LocationStatus.NotSharing),
        ),
        places = listOf(home, school),
        isSharing = true,
        hasPermission = true,
        hasBackgroundPermission = true,
        instant = InstantAlerts(hasApp = true, connected = true),
    )

    private fun shoot(name: String, dark: Boolean) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    MapContent(
                        state = state,
                        onMyWay = OnMyWay(others = listOf("Ana" to Instant.now().minusSeconds(300))),
                        error = null,
                        actions = MapActions(),
                        map = { _, modifier ->
                            Box(modifier.background(Color(0xFFDCE6DD)), contentAlignment = Alignment.Center) { Text("(map)") }
                        },
                    )
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun parent() {
        shoot("tab-map", dark = false)
        compose.onNodeWithText("School").assertExists()
    }

    @Test
    fun parentDark() = shoot("tab-map-dark", dark = true)
}
