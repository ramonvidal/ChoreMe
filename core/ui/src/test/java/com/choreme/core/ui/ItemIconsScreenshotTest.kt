package com.choreme.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.grocery.GroceryIcon
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Every chore and grocery picture, in light and dark, saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h1400dp")
class ItemIconsScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun light() = shoot(dark = false, "icons-light")

    @Test
    fun dark() = shoot(dark = true, "icons-dark")

    private fun shoot(dark: Boolean, name: String) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Chores", style = MaterialTheme.typography.titleMedium)
                        Gallery(ChoreTemplate.entries.map { it.name to it.icon })
                        Text("Groceries", style = MaterialTheme.typography.titleMedium)
                        Gallery(GroceryIcon.entries.map { it.name to it.drawable })
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}

@Composable
private fun Gallery(icons: List<Pair<String, Int>>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        icons.forEach { (label, icon) ->
            Row(Modifier.width(120.dp), verticalAlignment = Alignment.CenterVertically) {
                ItemIcon(icon, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label.lowercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
