package com.choreme.feature.groceries

import com.choreme.core.ui.R as CoreUiR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.key
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.grocery.CategoryGroup
import com.choreme.core.model.grocery.FamilyLists
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryIcons
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.QuickAddResult
import com.choreme.core.ui.EmptyState
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.HeaderLine
import com.choreme.core.ui.PillTab
import com.choreme.core.ui.TabHeader
import com.choreme.core.ui.drawable
import kotlinx.coroutines.launch

@Composable
internal fun GroceriesScreen(viewModel: GroceriesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    // In the shop, the screen shouldn't go dark between items.
    val view = LocalView.current
    DisposableEffect(state.shopping) {
        view.keepScreenOn = state.shopping
        onDispose { view.keepScreenOn = false }
    }
    GroceriesContent(
        state = state,
        query = query,
        actions = GroceriesActions(
            setNeeded = { item, needed -> viewModel.setNeeded(item, needed) },
            onQueryChange = { viewModel.onQueryChange(it) },
            submitQuery = { viewModel.submitQuery() },
            selectList = { viewModel.selectList(it) },
            pickSuggestion = { viewModel.pickSuggestion(it) },
            addStarterItems = { viewModel.addStarterItems() },
            restockStaples = { viewModel.restockStaples() },
            setShopping = { viewModel.setShopping(it) },
            createList = { viewModel.createList(it) },
            deleteShownList = { viewModel.deleteShownList() },
            save = { viewModel.save(it) },
            delete = { viewModel.delete(it) },
        ),
    )
}

/** What the Groceries tab can ask for; [GroceriesScreen] passes the view model's, tests pass their own. */
internal class GroceriesActions(
    val setNeeded: (GroceryItem, Boolean) -> Unit = { _, _ -> },
    val onQueryChange: (String) -> Unit = {},
    val submitQuery: () -> QuickAddResult? = { null },
    val selectList: (String?) -> Unit = {},
    val pickSuggestion: (GroceryItem) -> Unit = {},
    val addStarterItems: () -> Unit = {},
    val restockStaples: () -> Unit = {},
    val setShopping: (Boolean) -> Unit = {},
    val createList: (String) -> Unit = {},
    val deleteShownList: () -> Unit = {},
    val save: (GroceryItem) -> Unit = {},
    val delete: (GroceryItem) -> Unit = {},
)

/** The whole tab, drawn from [state] alone, so it can be screenshotted without Firebase. */
@Composable
internal fun GroceriesContent(state: GroceriesUiState, query: String, actions: GroceriesActions) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var editing by remember { mutableStateOf<GroceryItem?>(null) }
    val shopping = state.shopping
    var newList by rememberSaveable { mutableStateOf(false) }
    var deletingList by rememberSaveable { mutableStateOf(false) }

    fun markBought(item: GroceryItem) {
        actions.setNeeded(item, false)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = context.getString(R.string.grocery_bought, item.name),
                actionLabel = context.getString(CoreUiR.string.undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) actions.setNeeded(item, true)
        }
    }

    fun submit() {
        val result = actions.submitQuery()
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
            item(key = "header") {
                TabHeader(
                    title = state.listName ?: stringResource(R.string.list_groceries),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                ) { HeaderLine(pluralStringResource(R.plurals.grocery_to_buy_count, state.toBuyCount, state.toBuyCount)) }
            }
            item(key = "lists") {
                ListPicker(
                    state = state,
                    onSelect = actions.selectList,
                    onNew = { newList = true },
                    onDelete = { deletingList = true },
                )
            }
            if (state.othersShopping.isNotEmpty()) {
                item(key = "others-shopping") {
                    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Text(
                            stringResource(R.string.shopping_now, state.othersShopping.joinToString(", ")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
            item(key = "quick-add") {
                QuickAddField(query = query, onQueryChange = actions.onQueryChange, onSubmit = ::submit)
            }
            if (state.suggestions.isNotEmpty()) {
                item(key = "suggestions") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 8.dp),
                    ) {
                        state.suggestions.forEach { item ->
                            ItemChip(item, onClick = { actions.pickSuggestion(item) }, onLongClick = { editing = item })
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
                state.catalog.isEmpty() && state.isGroceries -> item(key = "empty") { EmptyCatalog(onAddStarter = actions.addStarterItems) }
                state.catalog.isEmpty() -> item(key = "empty-list") { EmptyState(title = stringResource(R.string.list_empty)) }
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
                                TextButton(onClick = actions.restockStaples, enabled = state.staplesAtHome > 0) {
                                    Text(stringResource(R.string.grocery_restock, state.staplesAtHome))
                                }
                            }
                        }
                    }
                    item(key = "shopping-toggle") {
                        FilterChip(
                            selected = shopping,
                            onClick = { actions.setShopping(!shopping) },
                            leadingIcon = { ItemIcon(CoreUiR.drawable.ic_shopping_cart, Modifier.size(FilterChipDefaults.IconSize)) },
                            label = { Text(stringResource(if (shopping) R.string.grocery_shopping_on else R.string.grocery_shopping_off)) },
                        )
                    }
                    if (state.toBuy.isEmpty()) {
                        item(key = "to-buy-empty") { EmptyState(title = stringResource(R.string.grocery_nothing_to_buy)) }
                    }
                    // One white card per aisle, rows split by thin lines, like the rest of the app.
                    state.toBuy.forEach { group ->
                        item(key = "to-buy-${group.category}") { CategoryHeader(group.category) }
                        item(key = "to-buy-card-${group.category}") {
                            Card(Modifier.fillMaxWidth()) {
                                group.items.forEachIndexed { index, item ->
                                    key(item.id) {
                                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(start = 56.dp))
                                        SwipeToBuy(onBought = { markBought(item) }) {
                                            ToBuyRow(item, large = shopping, onBought = { markBought(item) }, onEdit = { editing = item })
                                        }
                                    }
                                }
                            }
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
                        state.atHome.forEach { group -> item(key = "home-${group.category}") { AtHomeGroup(group, onNeeded = { actions.setNeeded(it, true) }, onEdit = { editing = it }) } }
                    }
                }
            }
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (newList) {
        NewListDialog(
            onCreate = {
                actions.createList(it)
                newList = false
            },
            onDismiss = { newList = false },
        )
    }
    if (deletingList) {
        AlertDialog(
            onDismissRequest = { deletingList = false },
            title = { Text(stringResource(R.string.list_delete_title, state.listName.orEmpty())) },
            text = { Text(stringResource(R.string.list_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.deleteShownList()
                    deletingList = false
                }) { Text(stringResource(CoreUiR.string.remove), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingList = false }) { Text(stringResource(CoreUiR.string.cancel)) } },
        )
    }

    editing?.let { item ->
        EditItemDialog(
            item = item,
            onSave = {
                actions.save(it)
                editing = null
            },
            onDelete = {
                actions.delete(item)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }
}

/** Groceries first, then the family's other lists, then "New list". Long-press a list to remove it. */
@Composable
private fun ListPicker(state: GroceriesUiState, onSelect: (String?) -> Unit, onNew: () -> Unit, onDelete: () -> Unit) {
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
private fun NewListDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
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
        shape = MaterialTheme.shapes.medium,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun EmptyCatalog(onAddStarter: () -> Unit) {
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

/**
 * In the store: the item's picture, its name, and a round tick to tap once it's in the cart.
 * Long-press to edit. Rows sit together in one card per aisle.
 */
@Composable
internal fun ToBuyRow(item: GroceryItem, large: Boolean, onBought: () -> Unit, onEdit: () -> Unit) {
    var ticked by remember(item.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .heightIn(min = if (large) 64.dp else 54.dp)
            .combinedClickable(onClick = { ticked = true; onBought() }, onLongClick = onEdit)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(item.icon, Modifier.size(if (large) 26.dp else 22.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(18.dp))
        Text(
            item.name,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (item.isStaple) {
            Spacer(Modifier.width(8.dp))
            UsualTag()
        }
        Spacer(Modifier.weight(1f))
        TickCircle(ticked, large)
    }
}

/** An empty ring that fills green with a tick when tapped. */
@Composable
private fun TickCircle(ticked: Boolean, large: Boolean) {
    val size = if (large) 30.dp else 26.dp
    if (ticked) {
        ItemIcon(CoreUiR.drawable.ic_check_circle, Modifier.size(size), tint = MaterialTheme.colorScheme.primary)
    } else {
        Box(Modifier.size(size).padding(2.dp).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
    }
}

/** Swipe a row either way to tick it off (with Undo, like a tap). */
@Composable
private fun SwipeToBuy(onBought: () -> Unit, content: @Composable () -> Unit) {
    val bought by rememberUpdatedState(onBought)
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) bought()
            value != SwipeToDismissBoxValue.Settled
        },
        positionalThreshold = { it * 0.35f },
    )
    SwipeToDismissBox(
        state = state,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary).padding(horizontal = 20.dp),
                contentAlignment = if (state.dismissDirection == SwipeToDismissBoxValue.EndToStart) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                ItemIcon(CoreUiR.drawable.ic_check_circle, tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) { content() }
}

/** "usual": bought most weeks, so "Restock usuals" puts it back. Words, not a star nobody can read. */
@Composable
private fun UsualTag() {
    Text(
        stringResource(R.string.grocery_usual_tag),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

@Composable
private fun AtHomeGroup(group: CategoryGroup, onNeeded: (GroceryItem) -> Unit, onEdit: (GroceryItem) -> Unit) {
    Column {
        CategoryHeader(group.category)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            group.items.forEach { item ->
                ItemTile(item, onClick = { onNeeded(item) }, onLongClick = { onEdit(item) })
            }
        }
    }
}

/** At home: a big tile to tap when it runs out. Long-press to edit. */
@Composable
internal fun ItemTile(item: GroceryItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    Text(
        item.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

/** A suggestion while typing: tap to put it on the list. */
@Composable
internal fun ItemChip(item: GroceryItem, onClick: () -> Unit, onLongClick: () -> Unit) {
    val shape = CircleShape
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.clip(shape).combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.heightIn(min = 40.dp).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ItemIcon(item.icon, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(item.name, style = MaterialTheme.typography.bodyMedium)
            if (item.isStaple) {
                Spacer(Modifier.width(6.dp))
                UsualTag()
            }
        }
    }
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

@get:DrawableRes
private val GroceryItem.icon: Int get() = GroceryIcons.forItem(name, category).drawable
