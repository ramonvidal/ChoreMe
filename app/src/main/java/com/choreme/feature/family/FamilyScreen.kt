package com.choreme.feature.family

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.R
import com.choreme.ui.common.PlaceholderScreen

@Composable
fun FamilyScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_family),
        description = stringResource(R.string.family_placeholder),
    )
}
