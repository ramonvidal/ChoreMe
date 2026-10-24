package com.choreme.feature.groceries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.data.FamilyRepository
import com.choreme.data.GroceryRepository
import com.choreme.domain.grocery.CategoryGroup
import com.choreme.domain.grocery.GroceryItem
import com.choreme.domain.grocery.QuickAddResult
import com.choreme.domain.grocery.groupByCategory
import com.choreme.domain.grocery.quickAdd
import com.choreme.domain.grocery.suggestions
import com.choreme.domain.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GroceriesUiState(
    val loading: Boolean = true,
    val catalog: List<GroceryItem> = emptyList(),
    val toBuy: List<CategoryGroup> = emptyList(),
    val atHome: List<CategoryGroup> = emptyList(),
    val suggestions: List<GroceryItem> = emptyList(),
) {
    val toBuyCount get() = toBuy.sumOf { it.items.size }
    val staplesAtHome get() = catalog.count { it.isStaple && !it.isNeeded }
}

@HiltViewModel
class GroceriesViewModel @Inject constructor(
    families: FamilyRepository,
    private val groceries: GroceryRepository,
) : ViewModel() {
    private val me: StateFlow<SessionState.InFamily?> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val catalog = me
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { groceries.observeItems(it.familyId) }

    val uiState: StateFlow<GroceriesUiState> = combine(catalog, _query) { items, query ->
        GroceriesUiState(
            loading = false,
            catalog = items,
            toBuy = groupByCategory(items.filter { it.isNeeded }),
            atHome = groupByCategory(items.filter { !it.isNeeded }),
            suggestions = suggestions(query, items),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroceriesUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }

    /** Adds what was typed, or puts it back on the list if the family already has it. */
    fun submitQuery(): QuickAddResult {
        val result = quickAdd(_query.value, uiState.value.catalog)
        withFamily { familyId, memberId ->
            when (result) {
                is QuickAddResult.Create -> groceries.add(familyId, memberId, result.name, result.category)
                is QuickAddResult.MarkNeeded -> groceries.setNeeded(familyId, memberId, result.item.id, true)
                is QuickAddResult.AlreadyNeeded, QuickAddResult.Blank -> Unit
            }
        }
        if (result != QuickAddResult.Blank) _query.value = ""
        return result
    }

    fun setNeeded(item: GroceryItem, isNeeded: Boolean) = withFamily { familyId, memberId ->
        groceries.setNeeded(familyId, memberId, item.id, isNeeded)
    }

    /** Picking a suggestion is the same as typing its name in full. */
    fun pickSuggestion(item: GroceryItem) {
        setNeeded(item, true)
        _query.value = ""
    }

    fun restockStaples() = withFamily { familyId, memberId ->
        groceries.restockStaples(familyId, memberId, uiState.value.catalog)
    }

    fun addStarterItems() = withFamily { familyId, memberId ->
        groceries.addStarterItems(familyId, memberId, uiState.value.catalog)
    }

    fun save(item: GroceryItem) = withFamily { familyId, memberId ->
        groceries.update(familyId, memberId, item.copy(name = item.name.trim()))
    }

    fun delete(item: GroceryItem) = withFamily { familyId, _ -> groceries.delete(familyId, item.id) }

    private inline fun withFamily(block: (familyId: String, memberId: String) -> Unit) {
        val session = me.value ?: return
        block(session.familyId, session.memberId)
    }
}
