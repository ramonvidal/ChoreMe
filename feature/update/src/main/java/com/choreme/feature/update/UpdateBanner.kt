package com.choreme.feature.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * "ChoreMe 0.31 is available – Install". Takes no room when there's nothing to offer.
 * This feature's only public entry point; :app shows it at the top of the screen.
 */
@Composable
fun UpdateBanner(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    if (state == UpdateState.None) return

    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            when (val current = state) {
                UpdateState.None -> Unit
                is UpdateState.Available -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.update_available, current.update.versionName),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.later(current.update) }) { Text(stringResource(R.string.update_later)) }
                    Button(onClick = { viewModel.install(current.update) }) { Text(stringResource(R.string.update_install)) }
                }
                is UpdateState.NeedsPermission -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.update_allow), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Button(onClick = { context.startActivity(viewModel.allowInstallsIntent()) }) {
                        Text(stringResource(R.string.update_allow_button))
                    }
                }
                is UpdateState.Downloading -> {
                    Text(stringResource(R.string.update_downloading), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
                }
                is UpdateState.Failed -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.update_failed), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.install(current.update) }) { Text(stringResource(R.string.update_retry)) }
                }
            }
        }
    }
}
