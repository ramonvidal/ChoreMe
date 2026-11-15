package com.choreme.feature.map

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val MAP_ROUTE = "map"

fun NavGraphBuilder.mapScreen() {
    composable(MAP_ROUTE) { MapScreen() }
}
