package com.choreme.feature.groceries

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.choreme.core.model.grocery.FamilyLists
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryIcons
import com.choreme.core.model.grocery.GroceryItem
import java.util.Locale
import com.choreme.core.model.grocery.GroceryTranslations
import com.choreme.core.ui.EmptyState
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.PillTab
import com.choreme.core.ui.drawable

/** Groceries first, then the family's other lists, then "New list". Long-press a list to remove it. */
@Composable
internal fun ListPicker(state: GroceriesUiState, onSelect: (String?) -> Unit, onNew: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillTab(stringResource(R.string.list_groceries), state.isGroceries, onClick = { onSelect(null) })
        state.lists.forEach { list -> PillTab(list.name, state.listId == list.id, onClick = { onSelect(list.id) }) }
        PillTab("+ " + stringResource(R.string.list_new), selected = false, onClick = onNew)
        if (state.canDeleteList) {
            TextButton(onClick = onDelete) { Text(stringResource(R.string.list_delete), color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
internal fun NewListDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.list_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.list_new_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= FamilyLists.MAX_NAME) name = it },
                    placeholder = { Text(stringResource(R.string.list_new_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text(stringResource(CoreUiR.string.add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

@Composable
internal fun QuickAddField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.grocery_quick_add)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        trailingIcon = {
            IconButton(onClick = onSubmit, enabled = query.isNotBlank()) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(CoreUiR.string.add))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
internal fun EmptyCatalog(onAddStarter: () -> Unit) {
    EmptyState(title = stringResource(R.string.grocery_empty_title), body = stringResource(R.string.grocery_empty_body)) {
        Button(onClick = onAddStarter) { Text(stringResource(R.string.grocery_add_starter)) }
    }
}

@Composable
internal fun CategoryHeader(category: GroceryCategory) {
    Text(
        stringResource(category.label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
internal fun EditItemDialog(
    item: GroceryItem,
    onSave: (GroceryItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(item.id) { mutableStateOf(GroceryTranslations.inLanguage(item.name, Locale.getDefault().language)) }
    var category by rememberSaveable(item.id) { mutableStateOf(item.category) }
    var isStaple by rememberSaveable(item.id) { mutableStateOf(item.isStaple) }
    var urgent by rememberSaveable(item.id) { mutableStateOf(item.urgent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { ItemIcon(GroceryIcons.forItem(name, category).drawable) },
        title = { Text(stringResource(R.string.grocery_edit_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_NAME_LENGTH) name = it },
                    label = { Text(stringResource(R.string.grocery_item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = isStaple, onValueChange = { isStaple = it }, role = Role.Checkbox),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isStaple, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.grocery_staple))
                        Text(stringResource(R.string.grocery_staple_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
                // An essential that ran out: first on the list, in red, and on Casa until someone buys it.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = urgent, onValueChange = { urgent = it }, role = Role.Checkbox),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = urgent, onCheckedChange = null)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(stringResource(R.string.grocery_urgent), color = MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.grocery_urgent_hint), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(stringResource(R.string.grocery_category), style = MaterialTheme.typography.labelLarge)
                GroceryCategory.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = category == option, onClick = { category = option }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = category == option, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(option.label))
                    }
                }
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.grocery_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(item.copy(name = name, category = category, isStaple = isStaple, urgent = urgent)) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(CoreUiR.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

// Matches the limit in firestore.rules.
private const val MAX_NAME_LENGTH = 60
