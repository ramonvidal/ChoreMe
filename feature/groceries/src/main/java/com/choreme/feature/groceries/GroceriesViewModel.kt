package com.choreme.feature.groceries

import com.choreme.core.model.grocery.urgentToBuy
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.AnnouncementRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.GroceryRepository
import com.choreme.core.data.PushEndpointRepository
import com.choreme.core.model.AnnouncementKind
import com.choreme.core.model.Role
import com.choreme.core.model.currentFromOthers
import com.choreme.core.model.grocery.FamilyList
import com.choreme.core.model.grocery.FamilyLists
import com.choreme.core.model.grocery.GroceryNames
import com.choreme.core.model.grocery.CategoryGroup
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.QuickAddResult
import com.choreme.core.model.grocery.groupByCategory
import com.choreme.core.model.grocery.quickAdd
import com.choreme.core.model.grocery.suggestions
import com.choreme.core.model.session.SessionState
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

internal data class GroceriesUiState(
    val loading: Boolean = true,
    /** The family's lists besides groceries; [listId] null is the grocery list. */
    val lists: List<FamilyList> = emptyList(),
    val listId: String? = null,
    val canDeleteList: Boolean = false,
    /** This phone's person turned shopping mode on (shared as "at the shop"). */
    val shopping: Boolean = false,
    /** Others at the shop right now, by name. */
    val othersShopping: List<String> = emptyList(),
    /** Items on the list being shown. */
    val catalog: List<GroceryItem> = emptyList(),
    /** Essentials that ran out, shown first in red (see [GroceryItem.urgent]). */
    val urgent: List<GroceryItem> = emptyList(),
    val toBuy: List<CategoryGroup> = emptyList(),
    val atHome: List<CategoryGroup> = emptyList(),
    val suggestions: List<GroceryItem> = emptyList(),
) {
    val isGroceries get() = listId == null
    val listName get() = lists.firstOrNull { it.id == listId }?.name
    val toBuyCount get() = urgent.size + toBuy.sumOf { it.items.size }
    val staplesAtHome get() = catalog.count { it.isStaple && !it.isNeeded }
}

@HiltViewModel
class GroceriesViewModel @Inject constructor(
    families: FamilyRepository,
    private val groceries: GroceryRepository,
    private val announcements: AnnouncementRepository,
    private val endpoints: PushEndpointRepository,
    private val push: PushSender,
) : ViewModel() {
    private val me: StateFlow<SessionState.InFamily?> = families.inFamily
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _query = MutableStateFlow("")
    internal val query: StateFlow<String> = _query

    private val session = me.filterIsInstance<SessionState.InFamily>()
    private val allItems = session.flatMapLatest { groceries.observeItems(it.familyId) }
    private val lists = session.flatMapLatest { groceries.observeLists(it.familyId) }.onStart { emit(emptyList()) }
    private val selectedList = MutableStateFlow<String?>(null)

    /** Who's at the shop, refreshed every minute so old news drops off. */
    private val shoppers = session.flatMapLatest { s ->
        combine(announcements.observeCurrent(s.familyId), families.observeMembers(s.familyId)) { (news, now), members ->
            val names = members.associate { it.id to it.name }
            val mine = news.any { it.memberId == s.memberId && it.kind == AnnouncementKind.SHOPPING && it.isCurrent(now) }
            mine to news.currentFromOthers(AnnouncementKind.SHOPPING, s.memberId, now).mapNotNull { names[it.memberId] }
        }
    }.onStart { emit(false to emptyList()) }

    internal val uiState: StateFlow<GroceriesUiState> = combine(
        allItems,
        _query,
        combine(lists, selectedList, ::Pair),
        shoppers,
        session,
    ) { everything, query, (familyLists, chosen), (shopping, others), s ->
        // A list someone else deleted falls back to groceries.
        val listId = chosen?.takeIf { id -> familyLists.any { it.id == id } }
        val items = everything.filter { it.listId == listId }
        val list = familyLists.firstOrNull { it.id == listId }
        GroceriesUiState(
            loading = false,
            lists = familyLists,
            listId = listId,
            canDeleteList = list != null && (s.role == Role.PARENT || list.createdBy == s.memberId),
            shopping = shopping,
            othersShopping = others,
            catalog = items,
            // Essentials that ran out sit apart, first; the rest by aisle.
            urgent = items.urgentToBuy(),
            toBuy = groupByCategory(items.filter { it.isNeeded && !it.urgent }),
            atHome = groupByCategory(items.filter { !it.isNeeded }),
            suggestions = suggestions(query, items),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroceriesUiState())

    internal fun selectList(listId: String?) {
        selectedList.value = listId
        _query.value = ""
    }

    /** Opens the list with that name, making it first if the family doesn't have one yet. */
    internal fun createList(name: String) = withFamily { familyId, memberId ->
        val trimmed = name.trim().take(FamilyLists.MAX_NAME)
        if (trimmed.isEmpty()) return@withFamily
        val existing = uiState.value.lists.firstOrNull { GroceryNames.key(it.name) == GroceryNames.key(trimmed) }
        selectList(existing?.id ?: groceries.createList(familyId, memberId, trimmed))
    }

    internal fun deleteShownList() = withFamily { familyId, _ ->
        val listId = uiState.value.listId ?: return@withFamily
        groceries.deleteList(familyId, listId, uiState.value.catalog)
        selectList(null)
    }

    /** Shopping mode also tells the family "I'm at the shop", so they can add what's missing. */
    internal fun setShopping(on: Boolean) = withFamily { familyId, memberId ->
        if (on) {
            announcements.announce(familyId, memberId, AnnouncementKind.SHOPPING)
            viewModelScope.launch {
                try {
                    push.ping(endpoints.othersThan(familyId, memberId), PushKinds.SHOPPING)
                } catch (e: Exception) {
                    Log.w("ChoreMe", "Couldn't ping the family", e)
                }
            }
        } else {
            announcements.clear(familyId, memberId, AnnouncementKind.SHOPPING)
        }
    }

    internal fun onQueryChange(query: String) {
        _query.value = query
    }

    /** Adds what was typed, or puts it back on the list if the family already has it. */
    internal fun submitQuery(): QuickAddResult {
        val result = quickAdd(_query.value, uiState.value.catalog)
        withFamily { familyId, memberId ->
            when (result) {
                is QuickAddResult.Create -> groceries.add(familyId, memberId, result.name, result.category, listId = uiState.value.listId)
                is QuickAddResult.MarkNeeded -> groceries.setNeeded(familyId, memberId, result.item.id, true)
                is QuickAddResult.AlreadyNeeded, QuickAddResult.Blank -> Unit
            }
        }
        if (result != QuickAddResult.Blank) _query.value = ""
        return result
    }

    internal fun setNeeded(item: GroceryItem, isNeeded: Boolean) = withFamily { familyId, memberId ->
        groceries.setNeeded(familyId, memberId, item.id, isNeeded)
    }

    /** Picking a suggestion is the same as typing its name in full. */
    internal fun pickSuggestion(item: GroceryItem) {
        setNeeded(item, true)
        _query.value = ""
    }

    internal fun restockStaples() = withFamily { familyId, memberId ->
        groceries.restockStaples(familyId, memberId, uiState.value.catalog)
    }

    internal fun addStarterItems() = withFamily { familyId, memberId ->
        groceries.addStarterItems(familyId, memberId, uiState.value.catalog, Locale.getDefault().language)
    }

    internal fun save(item: GroceryItem) = withFamily { familyId, memberId ->
        groceries.update(familyId, memberId, item.copy(name = item.name.trim()))
        // "Urgent" is its own field, written apart (marking it also puts the item on the list).
        val before = uiState.value.catalog.firstOrNull { it.id == item.id }
        if (before != null && before.urgent != item.urgent) groceries.setUrgent(familyId, memberId, item.id, item.urgent)
    }

    internal fun setUrgent(item: GroceryItem, urgent: Boolean) = withFamily { familyId, memberId ->
        groceries.setUrgent(familyId, memberId, item.id, urgent)
    }

    internal fun delete(item: GroceryItem) = withFamily { familyId, _ -> groceries.delete(familyId, item.id) }

    private inline fun withFamily(block: (familyId: String, memberId: String) -> Unit) {
        val session = me.value ?: return
        block(session.familyId, session.memberId)
    }
}
