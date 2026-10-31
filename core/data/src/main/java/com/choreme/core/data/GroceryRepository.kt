package com.choreme.core.data

import android.util.Log
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.CATEGORY
import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.IS_NEEDED
import com.choreme.core.data.remote.FirestoreSchema.IS_STAPLE
import com.choreme.core.data.remote.FirestoreSchema.LIST_ID
import com.choreme.core.data.remote.FirestoreSchema.NAME
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_AT
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_BY
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.data.remote.toGroceryItem
import com.choreme.core.model.grocery.FamilyList
import com.choreme.core.model.grocery.FamilyLists
import com.choreme.core.model.grocery.GroceryCategory
import com.choreme.core.model.grocery.GroceryItem
import com.choreme.core.model.grocery.GroceryNames
import com.choreme.core.model.grocery.GroceryTranslations
import com.choreme.core.model.grocery.StarterItems
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes don't wait for the server: Firestore applies them to its local cache at once
 * and syncs when there's signal, which is exactly what's needed in a supermarket basement.
 */
@Singleton
class GroceryRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun items(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.GROCERY_ITEMS)

    private fun lists(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.LISTS)

    /** Items on every list; each item's [GroceryItem.listId] says which (null for groceries). */
    fun observeItems(familyId: String): Flow<List<GroceryItem>> =
        items(familyId).documentsFlow().map { docs -> docs.map { it.toGroceryItem() } }

    /** [listId] null puts it on the grocery list. */
    fun add(
        familyId: String,
        memberId: String,
        name: String,
        category: GroceryCategory,
        isNeeded: Boolean = true,
        listId: String? = null,
    ) {
        val collection = items(familyId)
        val ref = FamilyLists.itemDocumentId(listId, name)?.let(collection::document) ?: collection.document()
        ref.set(itemData(name, category, isNeeded, isStaple = false, memberId, listId), SetOptions.merge()).logFailure()
    }

    /** The family's other lists (pharmacy, packing...), by name. */
    fun observeLists(familyId: String): Flow<List<FamilyList>> =
        lists(familyId).documentsFlow().map { docs ->
            docs.mapNotNull { doc ->
                FamilyList(doc.id, doc.getString(NAME) ?: return@mapNotNull null, doc.getString(CREATED_BY).orEmpty())
            }.sortedBy { it.name.lowercase() }
        }

    /** Returns the new list's id, so it can be shown straight away. */
    fun createList(familyId: String, memberId: String, name: String): String {
        val ref = lists(familyId).document()
        ref.set(mapOf(NAME to name.trim(), CREATED_BY to memberId)).logFailure()
        return ref.id
    }

    /** The list and everything on it. */
    fun deleteList(familyId: String, listId: String, catalog: List<GroceryItem>) {
        val batch = firestore.get().batch()
        catalog.filter { it.listId == listId }.forEach { batch.delete(items(familyId).document(it.id)) }
        batch.delete(lists(familyId).document(listId))
        batch.commit().logFailure()
    }

    fun setNeeded(familyId: String, memberId: String, itemId: String, isNeeded: Boolean) {
        items(familyId).document(itemId).update(change(memberId, IS_NEEDED to isNeeded)).logFailure()
    }

    fun update(familyId: String, memberId: String, item: GroceryItem) {
        items(familyId).document(item.id)
            .update(
                change(
                    memberId,
                    NAME to item.name,
                    CATEGORY to item.category.name,
                    IS_STAPLE to item.isStaple,
                ),
            )
            .logFailure()
    }

    fun delete(familyId: String, itemId: String) {
        items(familyId).document(itemId).delete().logFailure()
    }

    /** Puts every usual item that's at home back on the list. */
    fun restockStaples(familyId: String, memberId: String, catalog: List<GroceryItem>) {
        val toRestock = catalog.filter { it.isStaple && !it.isNeeded }
        if (toRestock.isEmpty()) return
        val batch = firestore.get().batch()
        toRestock.forEach { batch.update(items(familyId).document(it.id), change(memberId, IS_NEEDED to true)) }
        batch.commit().logFailure()
    }

    /**
     * Adds the starter catalog in the phone's [language] ("en" or "pt"), skipping anything the
     * family already has, in either language.
     */
    fun addStarterItems(familyId: String, memberId: String, catalog: List<GroceryItem>, language: String) {
        val batch = firestore.get().batch()
        StarterItems
            .map { (name, category) -> GroceryTranslations.inLanguage(name, language) to category }
            .filter { (name, _) -> catalog.none { GroceryTranslations.sameItem(it.name, name) } }
            .forEach { (name, category) ->
                val ref = items(familyId).document(checkNotNull(GroceryNames.documentId(name)))
                batch.set(ref, itemData(name, category, isNeeded = false, isStaple = false, memberId))
            }
        batch.commit().logFailure()
    }

    private fun itemData(
        name: String,
        category: GroceryCategory,
        isNeeded: Boolean,
        isStaple: Boolean,
        memberId: String,
        listId: String? = null,
    ) = mapOf(
        NAME to name,
        CATEGORY to category.name,
        IS_NEEDED to isNeeded,
        IS_STAPLE to isStaple,
        LIST_ID to listId,
        UPDATED_AT to FieldValue.serverTimestamp(),
        UPDATED_BY to memberId,
    )

    private fun change(memberId: String, vararg fields: Pair<String, Any>): Map<String, Any> =
        mapOf(*fields, UPDATED_AT to FieldValue.serverTimestamp(), UPDATED_BY to memberId)

    private fun Task<*>.logFailure() {
        reportFailure("Grocery write")
    }
}
