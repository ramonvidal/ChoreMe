package com.choreme.feature.rooms

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val ROOMS_ROUTE = "rooms"

fun NavGraphBuilder.roomsScreen() {
    composable(ROOMS_ROUTE) { RoomsScreen() }
}
