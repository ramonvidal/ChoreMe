package com.choreme.feature.groceries

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.R
import com.choreme.ui.common.PlaceholderScreen

@Composable
fun GroceriesScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_groceries),
        description = stringResource(R.string.groceries_placeholder),
    )
}
