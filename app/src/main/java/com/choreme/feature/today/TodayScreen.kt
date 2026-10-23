package com.choreme.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.R
import com.choreme.ui.common.PlaceholderScreen

@Composable
fun TodayScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_today),
        description = stringResource(R.string.today_placeholder),
    )
}
