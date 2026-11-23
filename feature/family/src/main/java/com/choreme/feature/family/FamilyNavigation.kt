package com.choreme.feature.family

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** This feature's only public entry point; everything else stays internal to the module. */
const val FAMILY_ROUTE = "family"

/** Set to true on this tab's back stack entry to open its Map sub-tab (Casa's first steps do it through :app). */
const val OPEN_MAP_KEY = "open-map"

/**
 * The Family tab: People (messages, notes, members) and the [map] as its second sub-tab. The map belongs to
 * `:feature:map`; `:app` hands it in, so the two features stay apart.
 */
fun NavGraphBuilder.familyScreen(map: @Composable () -> Unit) {
    composable(FAMILY_ROUTE) { entry ->
        val openMap by entry.savedStateHandle.getStateFlow(OPEN_MAP_KEY, false).collectAsStateWithLifecycle()
        FamilyTabs(map, openMap = openMap, onMapOpened = { entry.savedStateHandle[OPEN_MAP_KEY] = false })
    }
}
