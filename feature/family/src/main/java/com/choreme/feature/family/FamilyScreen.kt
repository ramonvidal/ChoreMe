package com.choreme.feature.family

import com.choreme.core.ui.R as CoreUiR
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import com.choreme.core.ui.HeaderLine
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.MemberAvatar
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.TabHeader

@Composable
internal fun FamilyScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    var showAddKid by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var awayFor by remember { mutableStateOf<Member?>(null) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            TabHeader(state.family?.name ?: stringResource(R.string.family_title), Modifier.padding(top = 4.dp)) {
                HeaderLine(pluralStringResource(R.plurals.family_member_count, state.members.size, state.members.size))
            }
        }

        item { SectionLabel(stringResource(R.string.message_title)) }
        item(key = "messages") {
            FamilyMessages(
                messages = state.messages,
                members = state.members,
                me = state.meMember,
                onSend = viewModel::sendMessage,
                onDelete = viewModel::deleteMessage,
            )
        }

        if (state.joinRequests.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.family_requests_title)) }
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

        item { SectionLabel(stringResource(R.string.notes_title)) }
        item(key = "notes") {
            NotesSection(
                notes = state.notes,
                members = state.members,
                me = state.meMember,
                onAdd = viewModel::addNote,
                onEdit = viewModel::editNote,
                onDelete = viewModel::deleteNote,
            )
        }

        item { SectionLabel(stringResource(R.string.family_members_title)) }
        item(key = "members") {
            Card(Modifier.fillMaxWidth()) {
                state.members.forEachIndexed { index, member ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    MemberRow(
                        member = member,
                        isMe = member.id == state.me?.memberId,
                        canRemove = state.isParent && member.id != state.me?.memberId,
                        onRemove = { memberToRemove = member },
                        away = awayLabel(member, state.today),
                        onAway = if (state.canSetAway(member)) ({ awayFor = member }) else null,
                    )
                }
            }
        }

        item { ErrorText(actionState.error) }

        item(key = "invite-actions") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.family?.let { family -> InviteButton(family) }
                if (state.isParent) {
                    OutlinedButton(
                        onClick = { showAddKid = true },
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) { Text(stringResource(R.string.family_add_kid)) }
                }
            }
        }

        if (state.isParent && state.week.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.week_title)) }
            item(key = "week") { WeekSection(state.week) }
        }

        item { SectionLabel(stringResource(R.string.this_phone_title)) }
        item(key = "this-phone") { PhoneSettings() }

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

    awayFor?.let { member ->
        AwayDialog(
            member = member,
            today = state.today,
            onSet = { until ->
                viewModel.setAway(member, until)
                awayFor = null
            },
            onDismiss = { awayFor = null },
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

/** "Invite · ABC234": shares the code (the share sheet shows how to join). */
@Composable
private fun InviteButton(family: Family) {
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
private fun MemberRow(
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
            Text("$role · $devices", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (away != null) Text(away, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
