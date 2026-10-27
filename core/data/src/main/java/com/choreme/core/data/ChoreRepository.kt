package com.choreme.core.data

import android.util.Log
import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.AT
import com.choreme.core.data.remote.FirestoreSchema.CHORE_ID
import com.choreme.core.data.remote.FirestoreSchema.CREATED_AT
import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.DONE_BY
import com.choreme.core.data.remote.FirestoreSchema.ON
import com.choreme.core.data.remote.FirestoreSchema.POINTS
import com.choreme.core.data.remote.FirestoreSchema.TITLE
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_AT
import com.choreme.core.data.remote.FirestoreSchema.UPDATED_BY
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.data.remote.toChore
import com.choreme.core.data.remote.toFields
import com.choreme.core.model.NameKeys
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreDone
import com.choreme.core.model.chore.SavedChore
import com.choreme.core.model.chore.completeLog
import com.google.firebase.Timestamp
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
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
    private fun family(familyId: String) = firestore.get().collection(FirestoreSchema.FAMILIES).document(familyId)
    private fun chores(familyId: String) = family(familyId).collection(FirestoreSchema.CHORES)
    private fun savedChores(familyId: String) = family(familyId).collection(FirestoreSchema.CHORE_TEMPLATES)
    private fun log(familyId: String) = family(familyId).collection(FirestoreSchema.CHORE_LOG)

    fun observeChores(familyId: String): Flow<List<Chore>> =
        chores(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toChore() } }

    fun create(familyId: String, memberId: String, chore: Chore) {
        chores(familyId).document()
            .set(chore.toFields() + mapOf(CREATED_BY to memberId, CREATED_AT to FieldValue.serverTimestamp()) + stamp(memberId))
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

    /** Chores reported after [after] (by the server's clock), for "Leo noticed: Wash dishes" alerts. */
    suspend fun reportedSince(familyId: String, after: Instant): List<Chore> =
        chores(familyId)
            .whereGreaterThan(CREATED_AT, Timestamp(after.epochSecond, after.nano))
            .get()
            .await()
            .documents
            .mapNotNull { it.toChore() }

    /** Chores changed after [after] (server clock), e.g. to find the ones just finished. */
    suspend fun changedSince(familyId: String, after: Instant): List<Chore> =
        chores(familyId)
            .whereGreaterThan(UPDATED_AT, Timestamp(after.epochSecond, after.nano))
            .get()
            .await()
            .documents
            .mapNotNull { it.toChore() }

    /** Notes down a finished chore for "who did what this week" (parents only, see firestore.rules). */
    fun logDone(familyId: String, done: ChoreDone) {
        log(familyId).document()
            .set(
                mapOf(
                    CHORE_ID to done.choreId,
                    TITLE to done.title,
                    DONE_BY to done.doneBy,
                    ON to done.on.toString(),
                    POINTS to done.points.toLong(),
                    AT to Instant.now().toEpochMilli(),
                ),
            )
            .logFailure()
    }

    /** Chores finished on [since] or later. */
    fun observeLog(familyId: String, since: LocalDate): Flow<List<ChoreDone>> =
        log(familyId).whereGreaterThanOrEqualTo(ON, since.toString()).documentsFlow().map { docs ->
            docs.mapNotNull { doc ->
                ChoreDone(
                    choreId = doc.getString(CHORE_ID).orEmpty(),
                    title = doc.getString(TITLE).orEmpty(),
                    doneBy = doc.getString(DONE_BY) ?: return@mapNotNull null,
                    on = doc.getString(ON)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null,
                    points = doc.getLong(POINTS)?.toInt() ?: 1,
                )
            }
        }

    /**
     * Finishes on [since] or later, including chores' own last finish when the log missed it
     * (finished before the log existed, or on a phone with an older app). See [completeLog].
     */
    fun observeFinished(familyId: String, since: LocalDate): Flow<List<ChoreDone>> =
        combine(observeLog(familyId, since).onStart { emit(emptyList()) }, observeChores(familyId)) { log, chores ->
            completeLog(log, chores).filter { !it.on.isBefore(since) }
        }

    fun observeSavedChores(familyId: String): Flow<List<SavedChore>> =
        savedChores(familyId).documentsFlow().map { docs ->
            docs.mapNotNull { doc ->
                val title = doc.getString(TITLE) ?: return@mapNotNull null
                SavedChore(doc.id, title, doc.getString(CREATED_BY).orEmpty(), doc.getLong(POINTS)?.toInt() ?: 1)
            }.sortedBy { it.title.lowercase() }
        }

    /** Keeps a hand-typed chore for next time; saving the same name twice keeps one. */
    fun saveForLater(familyId: String, memberId: String, title: String, points: Int = 1) {
        val ref = NameKeys.documentId(title)?.let(savedChores(familyId)::document) ?: return
        ref.set(mapOf(TITLE to title.trim(), CREATED_BY to memberId, POINTS to points.toLong())).logFailure()
    }

    /** A saved chore picked with other points keeps them for next time. */
    fun setSavedPoints(familyId: String, savedId: String, points: Int) {
        savedChores(familyId).document(savedId).update(POINTS, points.toLong()).logFailure()
    }

    fun deleteSaved(familyId: String, savedId: String) {
        savedChores(familyId).document(savedId).delete().logFailure()
    }

    private fun stamp(memberId: String) = mapOf(UPDATED_AT to FieldValue.serverTimestamp(), UPDATED_BY to memberId)

    private fun Task<*>.logFailure() {
        addOnFailureListener { Log.w("ChoreMe", "Chore write failed", it) }
    }
}
