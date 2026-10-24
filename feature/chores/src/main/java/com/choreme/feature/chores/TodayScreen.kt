package com.choreme.feature.chores

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.core.ui.PlaceholderScreen

@Composable
internal fun TodayScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.today_title),
        description = stringResource(R.string.today_placeholder),
    )
}
