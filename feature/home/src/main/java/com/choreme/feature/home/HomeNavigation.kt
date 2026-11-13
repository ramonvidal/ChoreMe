package com.choreme.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point besides [WeatherAlerts]; everything else stays internal to the module. */
const val HOME_ROUTE = "home"

/** The callbacks let the app take people to the chores and the grocery list without this feature knowing about them. */
fun NavGraphBuilder.homeScreen(onOpenChores: () -> Unit, onAddChore: () -> Unit, onOpenGroceries: () -> Unit) {
    composable(HOME_ROUTE) { HomeScreen(onOpenChores, onAddChore, onOpenGroceries) }
}
