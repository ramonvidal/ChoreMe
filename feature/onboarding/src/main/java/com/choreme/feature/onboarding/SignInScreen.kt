package com.choreme.feature.onboarding

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.data.requestGoogleIdToken
import com.choreme.core.ui.ErrorText
import com.choreme.core.ui.Mascot
import com.choreme.core.ui.NosButton
import com.choreme.core.ui.NosButtonStyle

@Composable
fun SignInScreen(viewModel: SignInViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(size = 140.dp)
        Text(stringResource(CoreUiR.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(
            stringResource(R.string.sign_in_tagline),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        NosButton(
            text = stringResource(R.string.sign_in_google),
            onClick = { viewModel.signInWithGoogle { requestGoogleIdToken(context) } },
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        NosButton(
            text = stringResource(R.string.sign_in_kid),
            onClick = viewModel::signInAsKid,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
            style = NosButtonStyle.Tonal,
        )
        Text(
            stringResource(R.string.sign_in_kid_hint),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        if (state.busy) CircularProgressIndicator()
        ErrorText(state.error)
    }
}
