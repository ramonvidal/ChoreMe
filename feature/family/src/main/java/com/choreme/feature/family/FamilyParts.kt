package com.choreme.feature.family

import com.choreme.core.ui.R as CoreUiR
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.Family
import com.choreme.core.model.JoinRequest
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar

/** "Invite · ABC234": shares the code (the share sheet shows how to join). */
@Composable
internal fun InviteButton(family: Family) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = { shareInvite(context, family) },
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.family_invite_button, family.inviteCode))
    }
}

private fun shareInvite(context: Context, family: Family) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.family_invite_message, family.name, family.inviteCode))
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** For a kid, the parent picks which profile this device belongs to, or makes a new one. */
@Composable
internal fun JoinRequestCard(
    request: JoinRequest,
    kidProfiles: List<Member>,
    enabled: Boolean,
    onApprove: (linkToKidId: String?) -> Unit,
    onDecline: () -> Unit,
) {
    var linkTo by rememberSaveable(request.uid) {
        mutableStateOf(kidProfiles.firstOrNull { it.name.equals(request.name, ignoreCase = true) }?.id)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(
                    if (request.role == Role.KID) R.string.request_wants_kid else R.string.request_wants_parent,
                    request.name,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(request.deviceName, style = MaterialTheme.typography.bodySmall)
            if (request.role == Role.KID && kidProfiles.isNotEmpty()) {
                Text(stringResource(R.string.request_link_to), style = MaterialTheme.typography.bodyMedium)
                ProfileOption(stringResource(R.string.request_new_profile, request.name), linkTo == null) { linkTo = null }
                kidProfiles.forEach { kid ->
                    ProfileOption(kid.name, linkTo == kid.id) { linkTo = kid.id }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onApprove(linkTo.takeIf { request.role == Role.KID }) }, enabled = enabled) {
                    Text(stringResource(CoreUiR.string.approve))
                }
                OutlinedButton(onClick = onDecline, enabled = enabled) { Text(stringResource(CoreUiR.string.decline)) }
            }
        }
    }
}

@Composable
private fun ProfileOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect, role = SemanticsRole.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

/** "Away until 30 Oct" as a small grey tag beside the name. */
@Composable
internal fun AwayTag(text: String) {
    Row(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(CoreUiR.drawable.ic_flight, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
}

@Composable
internal fun MemberRow(
    member: Member,
    isMe: Boolean,
    canRemove: Boolean,
    onRemove: () -> Unit,
    away: String? = null,
    onAway: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(member, size = 36.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            // The tag moves under the name when they don't fit side by side.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (isMe) stringResource(R.string.member_me, member.name) else member.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                if (away != null) AwayTag(away)
            }
            val role = stringResource(if (member.role == Role.PARENT) R.string.role_parent else R.string.role_kid)
            val devices = if (member.uids.isEmpty()) {
                stringResource(R.string.member_no_device)
            } else {
                pluralStringResource(R.plurals.member_devices, member.uids.size, member.uids.size)
            }
            Text("$role · $devices", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onAway != null) {
            IconButton(onClick = onAway) {
                ItemIcon(
                    CoreUiR.drawable.ic_flight,
                    tint = if (away != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(CoreUiR.string.remove))
            }
        }
    }
}

@Composable
internal fun AddKidDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.family_add_kid)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.family_add_kid_hint))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.kid_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text(stringResource(CoreUiR.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

@Composable
internal fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}
