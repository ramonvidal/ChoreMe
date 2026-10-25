package com.choreme.feature.family

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val FAMILY_ROUTE = "family"

/** [onOpenSettings] lets the app show this phone's settings without this feature knowing about them. */
fun NavGraphBuilder.familyScreen(onOpenSettings: () -> Unit) {
    composable(FAMILY_ROUTE) { FamilyScreen(onOpenSettings) }
}
