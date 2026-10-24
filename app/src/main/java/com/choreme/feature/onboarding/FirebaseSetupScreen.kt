package com.choreme.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.choreme.R
import com.choreme.ui.common.PlaceholderScreen

@Composable
fun FirebaseSetupScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.firebase_missing_title),
        description = stringResource(R.string.firebase_missing_body),
        showComingSoon = false,
    )
}
