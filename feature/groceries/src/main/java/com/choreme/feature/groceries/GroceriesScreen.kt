package com.choreme.feature.groceries

import com.choreme.core.ui.R as CoreUiR
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.choreme.core.model.grocery.GroceryIcons
import com.choreme.core.model.grocery.GroceryItem
import java.util.Locale
import androidx.compose.ui.platform.LocalConfiguration
import com.choreme.core.model.grocery.GroceryTranslations
import com.choreme.core.model.grocery.QuickAddResult
import com.choreme.core.ui.EmptyState
import com.choreme.core.ui.ItemIcon
import com.choreme.core.ui.HeaderLine
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
                message = context.getString(R.string.grocery_bought, GroceryTranslations.inLanguage(item.name, Locale.getDefault().language)),
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
                snackbar.showSnackbar(context.getString(R.string.grocery_already_on_list, GroceryTranslations.inLanguage(result.item.name, Locale.getDefault().language)))
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
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
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
                                // The count is in the header already.
                                stringResource(R.string.grocery_to_buy),
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

@get:DrawableRes
internal val GroceryItem.icon: Int get() = GroceryIcons.forItem(name, category).drawable

/** A known grocery's name in this phone's language ("Milk" is "Leite" in Baianês); others as typed. */
@Composable
internal fun groceryName(name: String): String {
    val language = LocalConfiguration.current.locales[0].language
    return remember(name, language) { GroceryTranslations.inLanguage(name, language) }
}
