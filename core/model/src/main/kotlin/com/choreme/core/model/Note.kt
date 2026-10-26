package com.choreme.core.model

import java.time.Instant

/** A note on the family board: "Wi-Fi password", "Grandma arrives Friday at 18:00". */
data class Note(val id: String, val text: String, val createdBy: String, val updatedAt: Instant) {
    /** Whoever wrote it can change it; parents can tidy up any. */
    fun canEdit(member: Member): Boolean = member.id == createdBy || member.role == Role.PARENT

    companion object {
        const val MAX_LENGTH = 300
    }
}

/** Newest first. */
fun List<Note>.board(): List<Note> = sortedByDescending { it.updatedAt }
