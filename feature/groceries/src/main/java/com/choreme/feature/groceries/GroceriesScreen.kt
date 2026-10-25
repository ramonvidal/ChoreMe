package com.choreme.feature.groceries

import com.choreme.core.ui.R as CoreUiR
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.grocery.CategoryGroup
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryIcons
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.QuickAddResult
import kotlinx.coroutines.launch

@Composable
internal fun GroceriesScreen(viewModel: GroceriesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<GroceryItem?>(null) }
    var shopping by rememberSaveable { mutableStateOf(false) }

    // In the shop, the screen shouldn't go dark between items.
    val view = LocalView.current
    DisposableEffect(shopping) {
        view.keepScreenOn = shopping
        onDispose { view.keepScreenOn = false }
    }

    fun markBought(item: GroceryItem) {
        viewModel.setNeeded(item, false)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.grocery_bought, item.name),
                actionLabel = context.getString(CoreUiR.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.setNeeded(item, true)
        }
    }

    fun submit() {
        val result = viewModel.submitQuery()
        if (result is QuickAddResult.AlreadyNeeded) {
            scope.launch {
                snackbar.currentSnackbarData?.dismiss()
                snackbar.showSnackbar(context.getString(R.string.grocery_already_on_list, result.item.name))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item(key = "quick-add") {
                QuickAddField(query = query, onQueryChange = viewModel::onQueryChange, onSubmit = ::submit)
            }
            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestions") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        state.suggestions.forEach { item ->
                            ItemChip(item, onClick = { viewModel.pickSuggestion(item) }, onLongClick = { editing = item })
                        }
                    }
                }
            }

            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.catalog.isEmpty() -> item(key = "empty") { EmptyCatalog(onAddStarter = viewModel::addStarterItems) }
                else -> {
                    item(key = "to-buy-header") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.grocery_to_buy, state.toBuyCount),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.weight(1f),
                            )
                            if (!shopping) {
                                TextButton(onClick = viewModel::restockStaples, enabled = state.staplesAtHome > 0) {
                                    Text(stringResource(R.string.grocery_restock, state.staplesAtHome))
                                }
                            }
                        }
                    }
                    item(key = "shopping-toggle") {
                        FilterChip(
                            selected = shopping,
                            onClick = { shopping = !shopping },
                            label = { Text(stringResource(if (shopping) R.string.grocery_shopping_on else R.string.grocery_shopping_off)) },
                        )
                    }
                    if (state.toBuy.isEmpty()) {
                        item(key = "to-buy-empty") {
                            Text(
                                stringResource(R.string.grocery_nothing_to_buy),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                    state.toBuy.forEach { group ->
                        item(key = "to-buy-${group.category}") { CategoryHeader(group.category) }
                        items(group.items, key = { "need-${it.id}" }) { item ->
                            ToBuyRow(item, large = shopping, onBought = { markBought(item) }, onEdit = { editing = item })
                        }
                    }

                    // While shopping only the list matters; "at home" comes back afterwards.
                    if (state.atHome.isNotEmpty() && !shopping) {
                        item(key = "at-home-header") {
                            Column(Modifier.padding(top = 24.dp)) {
                                HorizontalDivider()
                                Text(
                                    stringResource(R.string.grocery_at_home),
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(top = 16.dp),
                                )
                                Text(stringResource(R.string.grocery_at_home_hint), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        state.atHome.forEach { group -> item(key = "home-${group.category}") { AtHomeGroup(group, viewModel, onEdit = { editing = it }) } }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    editing?.let { item ->
        EditItemDialog(
            item = item,
            onSave = {
                viewModel.save(it)
                editing = null
            },
            onDelete = {
                viewModel.delete(item)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun QuickAddField(query: String, onQueryChange: (String) -> Unit, onSubmit: () -> Unit) {
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
    )
}

@Composable
private fun EmptyCatalog(onAddStarter: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.grocery_empty_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.grocery_empty_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAddStarter) { Text(stringResource(R.string.grocery_add_starter)) }
        }
    }
}

@Composable
private fun CategoryHeader(category: GroceryCategory) {
    Text(
        "${GroceryIcons.forCategory(category)}  ${stringResource(category.label)}",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** In the store: tick it once it's in the cart. Long-press to edit. */
@Composable
private fun ToBuyRow(item: GroceryItem, large: Boolean, onBought: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (large) 64.dp else 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClick = onBought, onLongClick = onEdit),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = false, onCheckedChange = { onBought() })
        Text(item.icon, style = if (large) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Text(
            item.name,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        if (item.isStaple) StapleIcon()
    }
}

@Composable
private fun AtHomeGroup(group: CategoryGroup, viewModel: GroceriesViewModel, onEdit: (GroceryItem) -> Unit) {
    Column {
        CategoryHeader(group.category)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.items.forEach { item ->
                ItemChip(item, onClick = { viewModel.setNeeded(item, true) }, onLongClick = { onEdit(item) })
            }
        }
    }
}

/** At home: tap when it runs out. */
@Composable
private fun ItemChip(item: GroceryItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        shape = shape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(item.icon)
            Spacer(Modifier.width(6.dp))
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            if (item.isStaple) {
                Spacer(Modifier.width(4.dp))
                StapleIcon()
            }
        }
    }
}

@Composable
private fun StapleIcon() {
    Icon(
        Icons.Filled.Star,
        contentDescription = stringResource(R.string.grocery_staple),
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun EditItemDialog(
    item: GroceryItem,
    onSave: (GroceryItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable(item.id) { mutableStateOf(item.name) }
    var category by rememberSaveable(item.id) { mutableStateOf(item.category) }
    var isStaple by rememberSaveable(item.id) { mutableStateOf(item.isStaple) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${GroceryIcons.forItem(name, category)}  ${stringResource(R.string.grocery_edit_title)}") },
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
                onClick = { onSave(item.copy(name = name, category = category, isStaple = isStaple)) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(CoreUiR.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(CoreUiR.string.cancel)) } },
    )
}

// Matches the limit in firestore.rules.
private const val MAX_NAME_LENGTH = 60

private val GroceryItem.icon: String get() = GroceryIcons.forItem(name, category)
