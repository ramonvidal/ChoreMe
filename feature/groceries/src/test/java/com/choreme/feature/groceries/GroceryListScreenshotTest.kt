package com.choreme.feature.groceries

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** "To buy" rows and "at home" chips, light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h900dp")
class GroceryListScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun item(name: String, category: GroceryCategory, needed: Boolean, staple: Boolean = false) =
        GroceryItem(id = name, name = name, category = category, isNeeded = needed, isStaple = staple)

    private val toBuy = listOf(
        item("Milk", GroceryCategory.DAIRY_EGGS, needed = true, staple = true),
        item("Eggs", GroceryCategory.DAIRY_EGGS, needed = true),
        item("Bananas", GroceryCategory.PRODUCE, needed = true),
        item("Orange juice", GroceryCategory.DRINKS, needed = true),
        item("Toilet paper", GroceryCategory.HOUSEHOLD, needed = true, staple = true),
    )
    private val atHome = listOf(
        item("Bread", GroceryCategory.BAKERY, needed = false, staple = true),
        item("Pasta", GroceryCategory.PANTRY, needed = false),
        item("Coffee", GroceryCategory.PANTRY, needed = false, staple = true),
        item("Kohlrabi", GroceryCategory.PRODUCE, needed = false),
    )

    @Test
    fun light() = shoot(dark = false, "groceries-light")

    @Test
    fun dark() = shoot(dark = true, "groceries-dark")

    private fun shoot(dark: Boolean, name: String) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                Surface {
                    Column(Modifier.padding(16.dp)) {
                        toBuy.forEach { ToBuyRow(it, large = false, onBought = {}, onEdit = {}) }
                        CategoryHeader(GroceryCategory.PANTRY)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            atHome.forEach { ItemChip(it, onClick = {}, onLongClick = {}) }
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
