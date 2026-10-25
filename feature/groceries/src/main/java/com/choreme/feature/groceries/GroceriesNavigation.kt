package com.choreme.feature.groceries

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import com.choreme.core.ui.DeepLinks

/** This feature's only public entry point; everything else stays internal to the module. */
const val GROCERIES_ROUTE = "groceries"

fun NavGraphBuilder.groceriesScreen() {
    composable(GROCERIES_ROUTE, deepLinks = listOf(navDeepLink { uriPattern = DeepLinks.GROCERIES })) { GroceriesScreen() }
}
