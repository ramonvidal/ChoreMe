package com.choreme.feature.groceries

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.grocery.FamilyList
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.groupByCategory
import com.choreme.core.ui.theme.ChoreMeTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The whole Groceries tab, at home and in the shop, light and dark; saved to build/outputs/roborazzi. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "en-w411dp-h1600dp")
class GroceriesTabScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private fun item(name: String, category: GroceryCategory, needed: Boolean, staple: Boolean = false) =
        GroceryItem(id = name, name = name, category = category, isNeeded = needed, isStaple = staple)

    private val catalog = listOf(
        item("Milk", GroceryCategory.DAIRY_EGGS, needed = true, staple = true),
        item("Eggs", GroceryCategory.DAIRY_EGGS, needed = true),
        item("Bananas", GroceryCategory.PRODUCE, needed = true),
        item("Tomatoes", GroceryCategory.PRODUCE, needed = true),
        item("Toilet paper", GroceryCategory.HOUSEHOLD, needed = true, staple = true),
        item("Bread", GroceryCategory.BAKERY, needed = false, staple = true),
        item("Pasta", GroceryCategory.PANTRY, needed = false),
        item("Coffee", GroceryCategory.PANTRY, needed = false, staple = true),
        item("Apples", GroceryCategory.PRODUCE, needed = false),
    )
    private val state = GroceriesUiState(
        loading = false,
        lists = listOf(FamilyList("p", "Pharmacy", "mom")),
        catalog = catalog,
        toBuy = groupByCategory(catalog.filter { it.isNeeded }),
        atHome = groupByCategory(catalog.filter { !it.isNeeded }),
    )

    private fun shoot(name: String, state: GroceriesUiState, dark: Boolean, fontScale: Float = 1f) {
        compose.setContent {
            ChoreMeTheme(darkTheme = dark, dynamicColor = false) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                Surface(Modifier.fillMaxSize()) { GroceriesContent(state, query = "", actions = GroceriesActions()) }
            }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun atHome() {
        shoot("tab-groceries", state, dark = false)
        compose.onNodeWithText("Milk").assertExists()
    }

    @Test
    fun atHomeDark() = shoot("tab-groceries-dark", state, dark = true)

    @Test
    fun largeText() {
        shoot("tab-groceries-large-text", state, dark = false, fontScale = 1.5f)
    }

    @Test
    fun nothingToBuy() = shoot("tab-groceries-nothing-to-buy", state.copy(toBuy = emptyList()), dark = false)

    @Test
    fun shopping() = shoot("tab-groceries-shopping", state.copy(shopping = true, othersShopping = listOf("Leo")), dark = false)
}
