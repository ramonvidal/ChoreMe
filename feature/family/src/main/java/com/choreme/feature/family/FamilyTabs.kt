package com.choreme.feature.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.choreme.core.ui.SubTab
import com.choreme.core.ui.SubTabs
import kotlinx.coroutines.launch
import com.choreme.core.ui.R as CoreUiR

private enum class FamilyTab { PEOPLE, MAP }

/** People / Map under the top edge. */
@Composable
internal fun FamilySubTabs(selected: Int, onSelect: (Int) -> Unit) {
    SubTabs(
        listOf(
            SubTab(stringResource(R.string.family_tab_people), CoreUiR.drawable.ic_tab_people),
            SubTab(stringResource(R.string.family_tab_map), CoreUiR.drawable.ic_location_on),
        ),
        selected,
        onSelect,
    )
}

/**
 * The Family tab's two sub-tabs. They swipe from People to the Map, but not back: on the map a sideways drag moves the
 * map, so the way back is the sub-tab bar.
 */
@Composable
internal fun FamilyTabs(map: @Composable () -> Unit, openMap: Boolean = false, onMapOpened: () -> Unit = {}) {
    var savedTab by rememberSaveable { mutableIntStateOf(FamilyTab.PEOPLE.ordinal) }
    val pager = rememberPagerState(initialPage = savedTab) { FamilyTab.entries.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { savedTab = pager.currentPage }
    LaunchedEffect(openMap) {
        if (openMap) {
            pager.scrollToPage(FamilyTab.MAP.ordinal)
            onMapOpened()
        }
    }
    Column(Modifier.fillMaxSize()) {
        FamilySubTabs(pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        HorizontalPager(
            pager,
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = pager.currentPage == FamilyTab.PEOPLE.ordinal,
            // The map is heavy: build it only when its page is shown.
            beyondViewportPageCount = 0,
        ) { page ->
            when (FamilyTab.entries[page]) {
                FamilyTab.PEOPLE -> FamilyScreen()
                FamilyTab.MAP -> map()
            }
        }
    }
}
