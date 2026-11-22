package com.choreme.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.choreme.core.model.grocery.GroceryTranslations

/** A known grocery's name in this phone's language ("Milk" is "Leite" in Baianês); others as typed. */
@Composable
fun groceryName(name: String): String {
    val language = LocalConfiguration.current.locales[0].language
    return remember(name, language) { GroceryTranslations.inLanguage(name, language) }
}
