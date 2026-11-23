package com.choreme.feature.map

import androidx.compose.runtime.Composable

/**
 * This feature's public entry point besides [PlaceAlerts]: the map, shown by `:app` as the Map sub-tab of the Family
 * tab (a documented exception to the "one navigation entry" rule in CLAUDE.md). Everything else stays internal.
 */
@Composable
fun FamilyMapTab() {
    MapScreen()
}
