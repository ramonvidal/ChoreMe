package com.choreme.feature.chores

import com.choreme.core.ui.title
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreChoice
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.SavedChore
import com.choreme.core.model.chore.choreChoices
import com.choreme.core.model.chore.mostUsed
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The "add a chore" picker: most used and folded groups, one group open, and while typing. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1400dp")
class ChoicePickerScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.of(2026, 10, 31)
    private val saved = listOf(
        "Arrumar sala", "Feed fish", "Lixo Bio", "Lixo geral", "Lixo plastico",
        "Mandar email dono da casa sobre O2 e Boxen do banheiro", "Pegar passaporte na Rathaus",
        "Tomar remedio", "Tomar remedio Alinne", "Tomar remedio Ramon",
    ).map { SavedChore(it, it, "mom") }
    private val choices = choreChoices(saved)
    private val chores = listOf(
        Chore("1", "Tomar remedio Ramon", dueDate = today, doneCount = 20),
        Chore("2", "Lixo geral", dueDate = today, doneCount = 6),
        Chore("3", "Wash dishes", ChoreTemplate.DISHES, dueDate = today, doneCount = 4),
        Chore("4", "Arrumar sala", dueDate = today, doneCount = 2),
    )

    @Test
    fun picker() {
        compose.setContent {
            ChoreMeTheme(darkTheme = false, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        val titles = ChoreTemplate.entries.associateWith { stringResource(it.title) }
                        val titleOf = { c: ChoreChoice -> if (c is ChoreChoice.Saved) c.saved.title else titles.getValue(c.template) }
                        ChoicePicker(choices, mostUsed(choices, chores), query = "", titleOf = titleOf, isSelected = { false }, onPick = {}, onLongPress = {})
                        Text("While typing \"lix\":")
                        ChoicePicker(choices, emptyList(), query = "lix", titleOf = titleOf, isSelected = { false }, onPick = {}, onLongPress = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Bins").performClick()
        // In the open Bins group and in the "lix" results.
        compose.onAllNodesWithText("Take out organic waste").assertCountEquals(2)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/editor-picker.png")
    }
}
