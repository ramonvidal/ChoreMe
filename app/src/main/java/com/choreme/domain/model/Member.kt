package com.choreme.domain.model

enum class Role { PARENT, KID }

data class Member(
    val id: String,
    val name: String,
    val role: Role,
    /** Index into the member color palette. */
    val color: Int = 0,
    /** Signed-in devices linked to this member. Kids without a device have none. */
    val uids: List<String> = emptyList(),
)
