package com.choreme.feature.settings

import com.choreme.core.ui.R as CoreUiR
import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.AlertKind
import com.choreme.core.model.QuietHours
import com.choreme.core.push.PushRegistration
import com.choreme.core.ui.ItemIcon
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val endpoint by viewModel.pushEndpoint.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var picking by remember { mutableStateOf<Boolean?>(null) } // true: start, false: end
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.settings_hint), style = MaterialTheme.typography.bodySmall)
        }
        item { Section(R.string.alerts_section) }
        ALERTS.forEach { (kind, title, hint) ->
            item(key = kind.name) {
                SwitchRow(stringResource(title), stringResource(hint), settings.isEnabled(kind)) { viewModel.setEnabled(kind, it) }
            }
        }

        item { Section(R.string.quiet_section) }
        item {
            val quiet = settings.quietHours
            SwitchRow(stringResource(R.string.quiet_section), stringResource(R.string.quiet_hint), quiet != null) { on ->
                viewModel.setQuietHours(if (on) QuietHours.DEFAULT else null)
            }
            if (quiet != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picking = true }) { Text(stringResource(R.string.quiet_from, quiet.start.format(time))) }
                    OutlinedButton(onClick = { picking = false }) { Text(stringResource(R.string.quiet_to, quiet.end.format(time))) }
                }
            }
        }

        item { Section(R.string.instant_section) }
        item {
            if (endpoint != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ItemIcon(CoreUiR.drawable.ic_bolt, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.instant_on))
                }
            } else {
                Column {
                    Text(stringResource(R.string.instant_off), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PushRegistration.NTFY_PLAY_STORE_URL)))
                    }) { Text(stringResource(R.string.instant_install)) }
                }
            }
        }
    }

    val quiet = settings.quietHours
    val editingStart = picking
    if (quiet != null && editingStart != null) {
        val initial = if (editingStart) quiet.start else quiet.end
        val state = rememberTimePickerState(initial.hour, initial.minute, android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { picking = null },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = {
                    val chosen = LocalTime.of(state.hour, state.minute)
                    viewModel.setQuietHours(if (editingStart) quiet.copy(start = chosen) else quiet.copy(end = chosen))
                    picking = null
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }
}

private val ALERTS = listOf(
    Triple(AlertKind.NEW_CHORES, R.string.alert_new_chores, R.string.alert_new_chores_hint),
    Triple(AlertKind.CHORES_DONE, R.string.alert_chores_done, R.string.alert_chores_done_hint),
    Triple(AlertKind.APPROVALS, R.string.alert_approvals, R.string.alert_approvals_hint),
    Triple(AlertKind.JOIN_REQUESTS, R.string.alert_join_requests, R.string.alert_join_requests_hint),
    Triple(AlertKind.ARRIVALS, R.string.alert_arrivals, R.string.alert_arrivals_hint),
    Triple(AlertKind.MORNING_SUMMARY, R.string.alert_morning, R.string.alert_morning_hint),
)

@Composable
private fun Section(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
}

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
