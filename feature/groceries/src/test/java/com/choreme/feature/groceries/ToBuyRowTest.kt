package com.choreme.feature.groceries

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryItem
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class ToBuyRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val milk = GroceryItem(id = "milk", name = "Milk", category = GroceryCategory.DAIRY_EGGS, isNeeded = true, isStaple = false)

    @Test
    fun `shows the item, and a tap marks it bought`() {
        var bought = false
        compose.setContent { ToBuyRow(milk, large = false, onBought = { bought = true }, onEdit = {}) }

        compose.onNodeWithText("Milk").performClick()
        assertTrue(bought)
    }

    @Test
    fun `shopping mode shows the same item, bigger`() {
        compose.setContent { ToBuyRow(milk, large = true, onBought = {}, onEdit = {}) }
        compose.onNodeWithText("Milk").assertExists()
    }
}
