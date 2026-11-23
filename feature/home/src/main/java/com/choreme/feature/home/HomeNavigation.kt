package com.choreme.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point besides [WeatherAlerts]; everything else stays internal to the module. */
const val HOME_ROUTE = "home"

/**
 * The callbacks let the app take people to the chores, the grocery list, the settings, the map and the agenda (and its
 * "New event" dialog) without this feature knowing about them.
 */
fun NavGraphBuilder.homeScreen(
    onOpenChores: () -> Unit,
    onAddChore: () -> Unit,
    onOpenGroceries: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenAgenda: () -> Unit,
    onAddEvent: () -> Unit,
) {
    composable(HOME_ROUTE) { HomeScreen(onOpenChores, onAddChore, onOpenGroceries, onOpenSettings, onOpenMap, onOpenAgenda, onAddEvent) }
}
