package com.choreme.feature.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.choreme.core.ui.R as CoreUiR
import com.choreme.core.ui.SubTab
import com.choreme.core.ui.SubTabs

private enum class MapTab { MAP, TRIPS }

/** Map / Trips under the top edge. */
@Composable
internal fun MapSubTabs(selected: Int, onSelect: (Int) -> Unit) {
    SubTabs(
        listOf(
            SubTab(stringResource(R.string.tab_map), CoreUiR.drawable.ic_tab_map),
            SubTab(stringResource(R.string.tab_trips), CoreUiR.drawable.ic_tab_trips),
        ),
        selected,
        onSelect,
    )
}

/** The Map tab: the family on the map and its places, and the plan of regular trips the weather is told for. */
@Composable
internal fun MapTabs() {
    var tab by rememberSaveable { mutableStateOf(MapTab.MAP) }
    Column(Modifier.fillMaxSize()) {
        MapSubTabs(tab.ordinal) { tab = MapTab.entries[it] }
        Box(Modifier.weight(1f)) {
            when (tab) {
                MapTab.MAP -> MapScreen()
                MapTab.TRIPS -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { TripPlan() }
            }
        }
    }
}
