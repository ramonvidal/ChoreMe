package com.choreme.core.push

/**
 * Implemented by features that react to pings from other family phones, and bound into
 * Hilt with `@IntoSet`. A ping only says what kind of thing changed (e.g. [PushKinds.PLACE_EVENTS]);
 * listeners read the details from the family's database themselves.
 */
interface PushListener {
    fun onPing(kind: String)
}

object PushKinds {
    /** Someone arrived at or left a saved place. */
    const val PLACE_EVENTS = "place-events"

    /** Someone noticed something that needs doing. */
    const val CHORE_ADDED = "chore-added"

    /** A kid finished a chore; parents need to approve it. */
    const val CHORE_TO_APPROVE = "chore-to-approve"

    /** Someone asked to join the family; parents need to let them in. */
    const val JOIN_REQUEST = "join-request"
}
