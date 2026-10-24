package com.choreme.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.InviteCode
import com.choreme.core.model.session.SessionState
import com.choreme.core.ui.ErrorText

/** First run after signing in: parents create or join a family, kids can only ask to join. */
@Composable
fun OnboardingScreen(session: SessionState.NoFamily, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var yourName by rememberSaveable { mutableStateOf(session.suggestedName) }
    var familyName by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    // Anonymous devices are always kids; Google accounts can belong to kids too.
    var joinAsKid by rememberSaveable { mutableStateOf(session.isKid) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(if (session.isKid) R.string.onboarding_title_kid else R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (session.requestDeclined) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(stringResource(R.string.onboarding_declined), modifier = Modifier.padding(16.dp))
            }
        }
        OutlinedTextField(
            value = yourName,
            onValueChange = { yourName = it },
            label = { Text(stringResource(R.string.your_name)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )

        if (!session.isKid) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.onboarding_create_title), style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text(stringResource(R.string.family_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { viewModel.createFamily(familyName, yourName) },
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.onboarding_create)) }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.onboarding_join_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.onboarding_join_hint), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= InviteCode.LENGTH + 2) code = it },
                    label = { Text(stringResource(R.string.invite_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!session.isKid) {
                    Row(
                        modifier = Modifier.toggleable(value = joinAsKid, onValueChange = { joinAsKid = it }, role = Role.Checkbox),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = joinAsKid, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_join_as_kid))
                    }
                }
                Button(
                    onClick = { viewModel.join(code, yourName, asKid = joinAsKid) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.onboarding_join)) }
            }
        }

        if (state.busy) CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        ErrorText(state.error)
        TextButton(onClick = viewModel::signOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(if (session.isKid) R.string.onboarding_back else R.string.onboarding_switch_account))
        }
    }
}
