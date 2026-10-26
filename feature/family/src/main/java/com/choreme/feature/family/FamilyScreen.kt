package com.choreme.feature.family

import com.choreme.core.ui.R as CoreUiR
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.data.requestGoogleIdToken
import com.choreme.core.model.Family
import com.choreme.core.model.JoinRequest
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.ui.ErrorText
import com.choreme.core.ui.theme.memberColor

@Composable
internal fun FamilyScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    var showAddKid by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        state.family?.let { family -> item { InviteCard(family) } }

        if (state.joinRequests.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.family_requests_title)) }
            items(state.joinRequests, key = { "request-${it.uid}" }) { request ->
                JoinRequestCard(
                    request = request,
                    kidProfiles = state.kidProfiles,
                    enabled = !actionState.busy,
                    onApprove = { linkTo -> viewModel.approve(request, linkTo) },
                    onDecline = { viewModel.decline(request) },
                )
            }
        }

        item { SectionTitle(stringResource(R.string.family_members_title)) }
        items(state.members, key = { "member-${it.id}" }) { member ->
            MemberRow(
                member = member,
                isMe = member.id == state.me?.memberId,
                canRemove = state.isParent && member.id != state.me?.memberId,
                onRemove = { memberToRemove = member },
            )
        }

        item { ErrorText(actionState.error) }

        if (state.isParent) {
            item {
                OutlinedButton(onClick = { showAddKid = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.family_add_kid))
                }
            }
        }

        item {
            TextButton(
                onClick = {
                    if (viewModel.signOutNeedsApprovalAgain) confirmSignOut = true else viewModel.signOut()
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.sign_out)) }
        }

        item {
            HorizontalDivider()
            Text(
                stringResource(R.string.privacy_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(onClick = { confirmLeave = true }, enabled = !actionState.busy) {
                    Text(stringResource(R.string.leave_family))
                }
                TextButton(onClick = { confirmDelete = true }, enabled = !actionState.busy) {
                    Text(stringResource(R.string.delete_my_data), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmLeave) {
        ConfirmDialog(
            text = stringResource(if (state.members.size <= 1) R.string.leave_last_confirm else R.string.leave_confirm),
            confirmLabel = stringResource(R.string.leave_family),
            onConfirm = {
                confirmLeave = false
                viewModel.leave()
            },
            onDismiss = { confirmLeave = false },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            text = stringResource(R.string.delete_confirm),
            confirmLabel = stringResource(R.string.delete_my_data),
            onConfirm = {
                confirmDelete = false
                viewModel.deleteMyData { requestGoogleIdToken(context) }
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (showAddKid) {
        AddKidDialog(
            onAdd = { name ->
                viewModel.addKidProfile(name)
                showAddKid = false
            },
            onDismiss = { showAddKid = false },
        )
    }

    memberToRemove?.let { member ->
        ConfirmDialog(
            text = stringResource(R.string.family_remove_confirm, member.name),
            confirmLabel = stringResource(CoreUiR.string.remove),
            onConfirm = {
                viewModel.remove(member)
                memberToRemove = null
            },
            onDismiss = { memberToRemove = null },
        )
    }

    if (confirmSignOut) {
        ConfirmDialog(
            text = stringResource(R.string.sign_out_kid_warning),
            confirmLabel = stringResource(R.string.sign_out),
            onConfirm = {
                confirmSignOut = false
                viewModel.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun InviteCard(family: Family) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(family.name, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.family_invite_hint), style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    family.inviteCode,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { shareInvite(context, family) }) {
                    Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.family_share_invite))
                }
            }
        }
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
private fun JoinRequestCard(
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

@Composable
private fun MemberRow(member: Member, isMe: Boolean, canRemove: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(memberColor(member.color), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                member.name.take(1).uppercase(),
                color = MaterialTheme.colorScheme.surface,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isMe) stringResource(R.string.member_me, member.name) else member.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            val role = stringResource(if (member.role == Role.PARENT) R.string.role_parent else R.string.role_kid)
            val devices = if (member.uids.isEmpty()) {
                stringResource(R.string.member_no_device)
            } else {
                pluralStringResource(R.plurals.member_devices, member.uids.size, member.uids.size)
            }
            Text("$role · $devices", style = MaterialTheme.typography.bodySmall)
        }
        if (canRemove) {
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(CoreUiR.string.remove))
            }
        }
    }
}

@Composable
private fun AddKidDialog(onAdd: (String) -> Unit, onDismiss: () -> Unit) {
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
private fun ConfirmDialog(text: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}
