package com.choreme.core.model

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String,
)

/** Someone who entered the invite code and is waiting for a parent to let them in. */
data class JoinRequest(
    val uid: String,
    val name: String,
    val role: Role,
    val deviceName: String,
)
