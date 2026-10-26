package com.choreme.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h500dp")
class UpdatesRowTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `shows the version, checks on tap, and says what it found`() {
        var checked = false
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        UpdatesRow("0.31", UpdateCheck.Found("0.32"), onCheck = { checked = true })
                        UpdatesRow("0.32", UpdateCheck.UpToDate, onCheck = {})
                        UpdatesRow("0.32", UpdateCheck.Checking, onCheck = {})
                    }
                }
            }
        }
        compose.onNodeWithText("This phone has ChoreMe 0.31.").assertExists()
        compose.onNodeWithText("ChoreMe 0.32 is ready: tap Install at the top of the screen.").assertExists()
        compose.onNodeWithText("You have the newest version.").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/settings-updates.png")

        compose.onAllNodesWithText("Check for updates")[0].performClick()
        assertTrue(checked)
        // While checking, the button waits.
        compose.onAllNodesWithText("Check for updates")[2].assertIsNotEnabled()
    }
}
