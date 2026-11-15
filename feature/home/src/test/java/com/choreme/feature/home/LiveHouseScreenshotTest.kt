package com.choreme.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.home.Gag
import com.choreme.core.model.home.Occasion
import com.choreme.core.model.home.SceneInput
import com.choreme.core.model.weather.Sky
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The living house in its special dates and with its little events, still pictures; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h2400dp")
class LiveHouseScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun gallery(name: String, pictures: List<Pair<SceneInput, Pair<Gag?, Float>>>) {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        pictures.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                pair.forEach { (scene, gag) ->
                                    LiveHouse(scene, Modifier.weight(1f), animated = false, previewGag = gag.first, previewAt = gag.second)
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    private fun scene(hour: Float, sky: Sky = Sky.CLEAR, occasion: Occasion? = null, people: Int = 2, urgent: Boolean = false, wind: Float = 14f) =
        SceneInput(hour, sky, wind, people, urgent, occasion)

    /** Every special date, in the weather and at the hour that suits it. */
    @Test
    fun occasions() = gallery(
        "live-house-occasions",
        listOf(
            scene(22f, occasion = Occasion.HALLOWEEN) to (Gag.GHOST to 0.5f),
            scene(21f, Sky.SNOW, Occasion.CHRISTMAS) to (Gag.SLEIGH to 0.5f),
            scene(11f, occasion = Occasion.EASTER) to (Gag.BUNNY to 0.7f),
            scene(15f, occasion = Occasion.CARNIVAL) to (Gag.CONFETTI to 0.3f),
            scene(22f, occasion = Occasion.SAO_JOAO) to (Gag.FIREWORKS to 0.45f),
            scene(23f, occasion = Occasion.NEW_YEAR) to (Gag.FIREWORKS to 0.4f),
            scene(11f, occasion = Occasion.CHILDRENS_DAY) to (Gag.BALLOONS to 0.5f),
            scene(20.5f, occasion = Occasion.HALLOWEEN) to (Gag.BATS to 0.5f),
        ),
    )

    /** The ordinary little events, and the house in a few weathers. */
    @Test
    fun gags() = gallery(
        "live-house-gags",
        listOf(
            scene(11f) to (Gag.BIRD to 0.5f),
            scene(11f, Sky.PARTLY_CLOUDY) to (Gag.CAT to 0.5f),
            scene(11f) to (Gag.KITE to 0.5f),
            scene(15f, Sky.PARTLY_CLOUDY) to (Gag.RAINBOW to 0.5f),
            scene(23f) to (Gag.SHOOTING_STAR to 0.4f),
            scene(11f) to (Gag.BUTTERFLY to 0.4f),
            scene(22f, Sky.RAIN, urgent = true) to (null to 0f),
            scene(16f, Sky.STORM, people = 3) to (null to 0f),
        ),
    )
}
