package com.choreme.core.data

import android.util.Log
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_AT
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_BY
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.data.remote.toChore
import com.choreme.core.data.remote.toFields
import com.choreme.core.model.chore.Chore
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Like groceries, writes don't wait for the server. Changes are worked out by the
 * rules in :core:model (claim, complete, approve, ...) and only the fields that
 * changed are written.
 */
@Singleton
class ChoreRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun chores(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.CHORES)

    fun observeChores(familyId: String): Flow<List<Chore>> =
        chores(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toChore() } }

    fun create(familyId: String, memberId: String, chore: Chore) {
        chores(familyId).document()
            .set(chore.toFields() + mapOf(CREATED_BY to memberId) + stamp(memberId))
            .logFailure()
    }

    /** Writes what changed between [before] and [after]; nothing if they're the same. */
    fun save(familyId: String, memberId: String, before: Chore, after: Chore) {
        val old = before.toFields()
        val changed = after.toFields().filter { (key, value) -> old[key] != value }
        if (changed.isEmpty()) return
        chores(familyId).document(after.id).update(changed + stamp(memberId)).logFailure()
    }

    fun delete(familyId: String, choreId: String) {
        chores(familyId).document(choreId).delete().logFailure()
    }

    private fun stamp(memberId: String) = mapOf(UPDATED_AT to FieldValue.serverTimestamp(), UPDATED_BY to memberId)

    private fun Task<*>.logFailure() {
        addOnFailureListener { Log.w("ChoreMe", "Chore write failed", it) }
    }
}
