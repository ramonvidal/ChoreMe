package com.choreme.feature.groceries

import androidx.annotation.StringRes
import com.choreme.R
import com.choreme.domain.grocery.GroceryCategory

@get:StringRes
val GroceryCategory.label: Int
    get() = when (this) {
        GroceryCategory.PRODUCE -> R.string.category_produce
        GroceryCategory.BAKERY -> R.string.category_bakery
        GroceryCategory.MEAT_FISH -> R.string.category_meat_fish
        GroceryCategory.DAIRY_EGGS -> R.string.category_dairy_eggs
        GroceryCategory.PANTRY -> R.string.category_pantry
        GroceryCategory.FROZEN -> R.string.category_frozen
        GroceryCategory.DRINKS -> R.string.category_drinks
        GroceryCategory.SNACKS -> R.string.category_snacks
        GroceryCategory.HOUSEHOLD -> R.string.category_household
        GroceryCategory.PERSONAL_CARE -> R.string.category_personal_care
        GroceryCategory.OTHER -> R.string.category_other
    }
