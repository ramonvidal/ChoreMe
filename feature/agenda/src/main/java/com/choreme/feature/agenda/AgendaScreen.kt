package com.choreme.feature.agenda

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.core.ui.PlaceholderScreen

@Composable
internal fun AgendaScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.agenda_title),
        description = stringResource(R.string.agenda_placeholder),
    )
}
