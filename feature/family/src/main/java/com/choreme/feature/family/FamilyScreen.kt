package com.choreme.feature.family

import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.mutableIntStateOf
import com.choreme.core.ui.AddButton
import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.data.requestGoogleIdToken
import com.choreme.core.model.Family
import com.choreme.core.model.JoinRequest
import com.choreme.core.model.Member
import com.choreme.core.ui.ActionState
import com.choreme.core.ui.ErrorText
import com.choreme.core.ui.HeaderLine
import com.choreme.core.ui.SectionLabel
import com.choreme.core.ui.TabHeader
import java.time.LocalDate
import com.choreme.core.model.Note
import com.choreme.core.model.FamilyMessage

@Composable
internal fun FamilyScreen(viewModel: FamilyViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    FamilyContent(
        state = state,
        actionState = actionState,
        signOutNeedsApprovalAgain = viewModel.signOutNeedsApprovalAgain,
        actions = FamilyActions(
            sendMessage = { viewModel.sendMessage(it) },
            deleteMessage = { viewModel.deleteMessage(it) },
            approve = { request, linkTo -> viewModel.approve(request, linkTo) },
            decline = { viewModel.decline(it) },
            addNote = { viewModel.addNote(it) },
            editNote = { note, text -> viewModel.editNote(note, text) },
            deleteNote = { viewModel.deleteNote(it) },
            addKidProfile = { viewModel.addKidProfile(it) },
            setAway = { member, until -> viewModel.setAway(member, until) },
            remove = { viewModel.remove(it) },
            signOut = { viewModel.signOut() },
            leave = { viewModel.leave() },
            deleteMyData = { viewModel.deleteMyData { requestGoogleIdToken(context) } },
        ),
    )
}

/** What the Family tab can ask for; [FamilyScreen] passes the view model's, tests pass their own. */
internal class FamilyActions(
    val sendMessage: (String) -> Unit = {},
    val deleteMessage: (FamilyMessage) -> Unit = {},
    val approve: (JoinRequest, String?) -> Unit = { _, _ -> },
    val decline: (JoinRequest) -> Unit = {},
    val addNote: (String) -> Unit = {},
    val editNote: (Note, String) -> Unit = { _, _ -> },
    val deleteNote: (Note) -> Unit = {},
    val addKidProfile: (String) -> Unit = {},
    val setAway: (Member, LocalDate?) -> Unit = { _, _ -> },
    val remove: (Member) -> Unit = {},
    val signOut: () -> Unit = {},
    val leave: () -> Unit = {},
    val deleteMyData: () -> Unit = {},
)

/** The whole tab, drawn from [state] alone. */
@Composable
internal fun FamilyContent(
    state: FamilyUiState,
    actionState: ActionState,
    signOutNeedsApprovalAgain: Boolean,
    actions: FamilyActions,
) {
    var showAddKid by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    var memberToRemove by remember { mutableStateOf<Member?>(null) }
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var awayFor by remember { mutableStateOf<Member?>(null) }

    // Counts the "Add a note" taps, so the notes open their editor once per tap.
    var addNote by remember { mutableIntStateOf(0) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
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
                    onSend = actions.sendMessage,
                    onDelete = actions.deleteMessage,
                )
            }

            if (state.joinRequests.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.family_requests_title)) }
                items(state.joinRequests, key = { "request-${it.uid}" }) { request ->
                    JoinRequestCard(
                        request = request,
                        kidProfiles = state.kidProfiles,
                        enabled = !actionState.busy,
                        onApprove = { linkTo -> actions.approve(request, linkTo) },
                        onDecline = { actions.decline(request) },
                    )
                }
            }

            item { SectionLabel(stringResource(R.string.notes_title)) }
            item(key = "notes") {
                NotesSection(
                addRequest = addNote,
                    notes = state.notes,
                    members = state.members,
                    me = state.meMember,
                    onAdd = actions.addNote,
                    onEdit = actions.editNote,
                    onDelete = actions.deleteNote,
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
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

            if (state.isParent && state.pointsOn && state.week.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.week_title)) }
                item(key = "week") { WeekSection(state.week) }
            }

            item {
                TextButton(
                    onClick = {
                        if (signOutNeedsApprovalAgain) confirmSignOut = true else actions.signOut()
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
        AddButton(stringResource(R.string.notes_add), { addNote++ }, Modifier.align(Alignment.BottomEnd).padding(16.dp))
    }

    if (confirmLeave) {
        ConfirmDialog(
            text = stringResource(if (state.members.size <= 1) R.string.leave_last_confirm else R.string.leave_confirm),
            confirmLabel = stringResource(R.string.leave_family),
            onConfirm = {
                confirmLeave = false
                actions.leave()
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
                actions.deleteMyData()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    if (showAddKid) {
        AddKidDialog(
            onAdd = { name ->
                actions.addKidProfile(name)
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
                actions.setAway(member, until)
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
                actions.remove(member)
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
                actions.signOut()
            },
            onDismiss = { confirmSignOut = false },
        )
    }
}
