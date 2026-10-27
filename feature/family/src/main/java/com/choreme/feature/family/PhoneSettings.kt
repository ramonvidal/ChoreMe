package com.choreme.feature.family

import com.choreme.core.ui.R as CoreUiR
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
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

private enum class Fold { ALERTS, QUIET, INSTANT, UPDATES }

/** "This phone": each part folded to one line with its current state, opened with a tap. */
@Composable
internal fun PhoneSettings(viewModel: PhoneSettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val endpoint by viewModel.pushEndpoint.collectAsStateWithLifecycle()
    val updateCheck by viewModel.updateCheck.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var open by rememberSaveable { mutableStateOf<Fold?>(null) }
    var picking by remember { mutableStateOf<Boolean?>(null) } // true: start, false: end
    val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    val toggle = { fold: Fold -> open = if (open == fold) null else fold }

    Column {
        Text(stringResource(R.string.this_phone_hint), style = MaterialTheme.typography.bodySmall)

        val quiet = settings.quietHours
        FoldRow(
            title = stringResource(R.string.alerts_section),
            summary = stringResource(R.string.alerts_summary, ALERTS.count { settings.isEnabled(it.first) }, ALERTS.size),
            open = open == Fold.ALERTS,
            onToggle = { toggle(Fold.ALERTS) },
        ) {
            ALERTS.forEach { (kind, title, hint) ->
                SwitchRow(stringResource(title), stringResource(hint), settings.isEnabled(kind)) { viewModel.setEnabled(kind, it) }
            }
        }
        FoldRow(
            title = stringResource(R.string.quiet_section),
            summary = if (quiet == null) {
                stringResource(R.string.quiet_off)
            } else {
                stringResource(R.string.quiet_summary, quiet.start.format(time), quiet.end.format(time))
            },
            open = open == Fold.QUIET,
            onToggle = { toggle(Fold.QUIET) },
        ) {
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
        FoldRow(
            title = stringResource(R.string.instant_section),
            summary = stringResource(if (endpoint != null) R.string.instant_summary_on else R.string.instant_summary_off),
            open = open == Fold.INSTANT,
            onToggle = { toggle(Fold.INSTANT) },
        ) {
            if (endpoint != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ItemIcon(CoreUiR.drawable.ic_bolt, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.instant_on))
                }
            } else {
                Text(stringResource(R.string.instant_off), style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PushRegistration.NTFY_PLAY_STORE_URL)))
                }) { Text(stringResource(R.string.instant_install)) }
            }
        }
        FoldRow(
            title = stringResource(R.string.updates_section),
            summary = stringResource(R.string.updates_summary, viewModel.installedVersion),
            open = open == Fold.UPDATES,
            onToggle = { toggle(Fold.UPDATES) },
        ) {
            UpdatesRow(viewModel.installedVersion, updateCheck, onCheck = viewModel::checkForUpdates)
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

@Composable
private fun FoldRow(title: String, summary: String, open: Boolean, onToggle: () -> Unit, content: @Composable () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
        AnimatedVisibility(open) {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
        }
        HorizontalDivider()
    }
}

@Composable
internal fun UpdatesRow(installedVersion: String, check: UpdateCheck, onCheck: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.updates_installed, installedVersion), style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCheck, enabled = check != UpdateCheck.Checking) { Text(stringResource(R.string.updates_check)) }
            if (check == UpdateCheck.Checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        val result = when (check) {
            UpdateCheck.Idle, UpdateCheck.Checking -> null
            UpdateCheck.UpToDate -> stringResource(R.string.updates_up_to_date)
            is UpdateCheck.Found -> stringResource(R.string.updates_found, check.version)
            UpdateCheck.Failed -> stringResource(R.string.updates_failed)
        }
        if (result != null) Text(result, style = MaterialTheme.typography.bodySmall)
    }
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

private val ALERTS = listOf(
    Triple(AlertKind.NEW_CHORES, R.string.alert_new_chores, R.string.alert_new_chores_hint),
    Triple(AlertKind.CHORES_DONE, R.string.alert_chores_done, R.string.alert_chores_done_hint),
    Triple(AlertKind.APPROVALS, R.string.alert_approvals, R.string.alert_approvals_hint),
    Triple(AlertKind.JOIN_REQUESTS, R.string.alert_join_requests, R.string.alert_join_requests_hint),
    Triple(AlertKind.ARRIVALS, R.string.alert_arrivals, R.string.alert_arrivals_hint),
    Triple(AlertKind.MESSAGES, R.string.alert_messages, R.string.alert_messages_hint),
    Triple(AlertKind.ON_MY_WAY, R.string.alert_on_my_way, R.string.alert_on_my_way_hint),
    Triple(AlertKind.SHOPPING, R.string.alert_shopping, R.string.alert_shopping_hint),
    Triple(AlertKind.MORNING_SUMMARY, R.string.alert_morning, R.string.alert_morning_hint),
)
