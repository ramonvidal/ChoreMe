package com.choreme.feature.family

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.clickable
import com.choreme.core.ui.MemberAvatar
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.IconButton
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.FamilyMessage
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Message everyone": one line to type, a Send button, and the last messages under it.
 * Everyone's phone shows it as a notification. Tap your own (or, for parents, any) to remove it.
 */
@Composable
internal fun FamilyMessages(
    messages: List<FamilyMessage>,
    members: List<Member>,
    me: Member?,
    onSend: (String) -> Unit,
    onDelete: (FamilyMessage) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<FamilyMessage?>(null) }
    val names = members.associate { it.id to it.name }
    fun send() {
        if (text.isNotBlank()) {
            onSend(text)
            text = ""
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // One slim rounded line with a send arrow inside it, like a chat app.
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest, CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                .padding(start = 18.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= FamilyMessage.MAX_LENGTH) text = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                maxLines = 3,
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                decorationBox = { field ->
                    if (text.isEmpty()) {
                        Text(stringResource(R.string.message_placeholder), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    field()
                },
            )
            IconButton(onClick = ::send, enabled = text.isNotBlank()) {
                Icon(
                    painterResource(CoreUiR.drawable.ic_send),
                    contentDescription = stringResource(R.string.message_send),
                    tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                )
            }
        }

        if (messages.isNotEmpty()) {
            val shown = if (showAll) messages else messages.take(VISIBLE)
            Card(Modifier.fillMaxWidth()) {
                shown.forEachIndexed { index, message ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    val canRemove = me != null && (message.from == me.id || me.role == Role.PARENT)
                    val sender = members.firstOrNull { it.id == message.from }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canRemove) { removing = message }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                    if (sender != null) MemberAvatar(sender, size = 32.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row {
                            Text(
                                if (message.from == me?.id) stringResource(R.string.message_you) else names[message.from] ?: stringResource(R.string.message_someone),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                whenLabel(message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(message.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    }
                }
            }
            if (messages.size > VISIBLE) {
                TextButton(onClick = { showAll = !showAll }) {
                    Text(stringResource(if (showAll) R.string.message_show_less else R.string.message_show_all))
                }
            }
        }
    }

    removing?.let { message ->
        AlertDialog(
            onDismissRequest = { removing = null },
            text = { Text(stringResource(R.string.message_remove_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(message)
                    removing = null
                }) { Text(stringResource(CoreUiR.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removing = null }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }
}

/** "18:05" today, "Mon 18:05" earlier. */
private fun whenLabel(message: FamilyMessage): String {
    val at = message.at.atZone(ZoneId.systemDefault())
    val time = at.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))
    return if (at.toLocalDate() == LocalDate.now()) time else at.format(DateTimeFormatter.ofPattern("EEE")) + " " + time
}

private const val VISIBLE = 3
