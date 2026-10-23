package com.choreme.feature.agenda

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.R
import com.choreme.ui.common.PlaceholderScreen

@Composable
fun AgendaScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.nav_agenda),
        description = stringResource(R.string.agenda_placeholder),
    )
}
