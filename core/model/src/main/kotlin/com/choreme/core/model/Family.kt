package com.choreme.core.model

data class Family(
    val id: String,
    val name: String,
    val inviteCode: String,
    /**
     * The family chose to play with points: chores are worth points, the week's points are
     * shown, and "Well done" says how many. Off, points show nowhere. Missing means on.
     */
    val pointsEnabled: Boolean = true,
)

/** Someone who entered the invite code and is waiting for a parent to let them in. */
data class JoinRequest(
    val uid: String,
    val name: String,
    val role: Role,
    val deviceName: String,
)
