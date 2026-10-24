package com.choreme.core.data.remote

import com.choreme.core.data.remote.FirestoreSchema.APPROVED_BY
import com.choreme.core.data.remote.FirestoreSchema.CLAIMED_BY
import com.choreme.core.data.remote.FirestoreSchema.COMPLETED_BY
import com.choreme.core.data.remote.FirestoreSchema.CREATED_BY
import com.choreme.core.data.remote.FirestoreSchema.DUE_DATE
import com.choreme.core.data.remote.FirestoreSchema.EVERY_DAYS
import com.choreme.core.data.remote.FirestoreSchema.LAST_DONE_BY
import com.choreme.core.data.remote.FirestoreSchema.LAST_DONE_ON
import com.choreme.core.data.remote.FirestoreSchema.RECURRENCE_TYPE
import com.choreme.core.data.remote.FirestoreSchema.STATUS
import com.choreme.core.data.remote.FirestoreSchema.TEMPLATE
import com.choreme.core.data.remote.FirestoreSchema.TITLE
import com.choreme.core.data.remote.FirestoreSchema.WEEK_DAYS
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.google.firebase.firestore.DocumentSnapshot
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Every chore field, so changes can be written as a diff: kids may only touch a few
 * fields (see firestore.rules), so updates must not rewrite the rest.
 * Dates are ISO strings ("2026-10-24"): they sort correctly and have no time zone.
 */
internal fun Chore.toFields(): Map<String, Any?> = mapOf(
    TITLE to title,
    TEMPLATE to template.name,
    RECURRENCE_TYPE to when (recurrence) {
        Recurrence.Once -> "ONCE"
        is Recurrence.EveryDays -> "EVERY_DAYS"
        is Recurrence.Weekly -> "WEEKLY"
    },
    EVERY_DAYS to (recurrence as? Recurrence.EveryDays)?.days?.toLong(),
    WEEK_DAYS to (recurrence as? Recurrence.Weekly)?.days?.map { it.value.toLong() }?.sorted().orEmpty(),
    DUE_DATE to dueDate.toString(),
    STATUS to status.name,
    CLAIMED_BY to claimedBy,
    COMPLETED_BY to completedBy,
    APPROVED_BY to approvedBy,
    LAST_DONE_BY to lastDoneBy,
    LAST_DONE_ON to lastDoneOn?.toString(),
)

/** Null for documents that can't be read as a chore, so one bad document doesn't hide the rest. */
internal fun DocumentSnapshot.toChore(): Chore? = runCatching {
    val recurrence = when (getString(RECURRENCE_TYPE)) {
        "EVERY_DAYS" -> Recurrence.EveryDays(checkNotNull(getLong(EVERY_DAYS)).toInt())
        "WEEKLY" -> Recurrence.Weekly(
            (get(WEEK_DAYS) as List<*>).map { DayOfWeek.of((it as Number).toInt()) }.toSet(),
        )
        else -> Recurrence.Once
    }
    Chore(
        id = id,
        title = getString(TITLE).orEmpty(),
        template = runCatching { ChoreTemplate.valueOf(getString(TEMPLATE).orEmpty()) }.getOrDefault(ChoreTemplate.OTHER),
        recurrence = recurrence,
        dueDate = LocalDate.parse(getString(DUE_DATE)),
        status = ChoreStatus.valueOf(getString(STATUS).orEmpty()),
        claimedBy = getString(CLAIMED_BY),
        completedBy = getString(COMPLETED_BY),
        approvedBy = getString(APPROVED_BY),
        lastDoneBy = getString(LAST_DONE_BY),
        lastDoneOn = getString(LAST_DONE_ON)?.let(LocalDate::parse),
        // Written once when the chore is reported, never changed afterwards.
        reportedBy = getString(CREATED_BY),
    )
}.getOrNull()
