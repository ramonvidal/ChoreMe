package com.choreme.core.data

import com.choreme.core.data.remote.FirestoreSchema
import com.choreme.core.data.remote.FirestoreSchema.DAYS
import com.choreme.core.data.remote.FirestoreSchema.LEARNED
import com.choreme.core.data.remote.FirestoreSchema.MEMBER_ID
import com.choreme.core.data.remote.FirestoreSchema.MINUTES
import com.choreme.core.data.remote.FirestoreSchema.MODE
import com.choreme.core.data.remote.FirestoreSchema.PLACE_ID
import com.choreme.core.data.remote.FirestoreSchema.START
import com.choreme.core.data.remote.FirestoreSchema.TITLE
import com.choreme.core.data.remote.documentsFlow
import com.choreme.core.model.weather.PlanEntry
import com.choreme.core.model.weather.TripMode
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** The family's regular trips ("Monday 8:00, to Work, by bike") for the weather. Writes don't wait for the server. */
@Singleton
class PlanRepository @Inject constructor(
    private val firestore: Lazy<FirebaseFirestore>,
) {
    private fun plans(familyId: String) = firestore.get()
        .collection(FirestoreSchema.FAMILIES).document(familyId)
        .collection(FirestoreSchema.PLANS)

    fun observe(familyId: String): Flow<List<PlanEntry>> =
        plans(familyId).documentsFlow().map { docs -> docs.mapNotNull { it.toPlanEntry() } }

    /** Adds [entry] when its id is empty, otherwise replaces it. */
    fun save(familyId: String, entry: PlanEntry) {
        val ref = if (entry.id.isEmpty()) plans(familyId).document() else plans(familyId).document(entry.id)
        ref.set(entry.toFields()).reportFailure("Couldn't save the trip")
    }

    fun delete(familyId: String, entryId: String) {
        plans(familyId).document(entryId).delete().reportFailure("Couldn't delete the trip")
    }
}

private fun PlanEntry.toFields(): Map<String, Any?> = mapOf(
    MEMBER_ID to memberId,
    TITLE to title,
    // ISO day numbers: 1 is Monday, 7 is Sunday.
    DAYS to days.map { it.value.toLong() }.sorted(),
    START to start.toString(),
    MINUTES to minutes.toLong(),
    MODE to mode.name,
    PLACE_ID to placeId,
    LEARNED to learned,
)

private fun DocumentSnapshot.toPlanEntry(): PlanEntry? = runCatching {
    PlanEntry(
        id = id,
        memberId = checkNotNull(getString(MEMBER_ID)),
        title = checkNotNull(getString(TITLE)),
        days = (get(DAYS) as List<*>).map { DayOfWeek.of((it as Number).toInt()) }.toSet(),
        start = LocalTime.parse(checkNotNull(getString(START))),
        minutes = checkNotNull(getLong(MINUTES)).toInt(),
        mode = runCatching { TripMode.valueOf(getString(MODE).orEmpty()) }.getOrDefault(TripMode.OTHER),
        placeId = getString(PLACE_ID),
        learned = getBoolean(LEARNED) ?: false,
    )
}.getOrNull()
