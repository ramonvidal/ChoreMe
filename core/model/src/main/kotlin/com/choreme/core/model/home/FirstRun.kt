package com.choreme.core.model.home

/** The things a new family sets up once, in the order that helps most. */
enum class FirstStep { SAVE_HOME, SHARE_LOCATION, ALLOW_NOTIFICATIONS, PLAN_TRIP }

/** One step of the getting-started list and whether it is done. */
data class StepState(val step: FirstStep, val done: Boolean)

/** What the phone knows about the setup. */
data class SetupFacts(
    /** At least one place is saved on the map. */
    val hasPlace: Boolean,
    /** This phone shares its location with the family. */
    val sharing: Boolean,
    /** Notifications are allowed (always true before Android 13, which asks for nothing). */
    val notificationsAllowed: Boolean,
    /** The family has at least one regular trip in its plan. */
    val hasTrip: Boolean,
    val isParent: Boolean,
)

/**
 * The getting-started list for this person. Saving places and planning trips are parents' work, so kids only get the
 * location and notification steps.
 */
fun firstRunSteps(facts: SetupFacts): List<StepState> = buildList {
    if (facts.isParent) add(StepState(FirstStep.SAVE_HOME, facts.hasPlace))
    add(StepState(FirstStep.SHARE_LOCATION, facts.sharing))
    add(StepState(FirstStep.ALLOW_NOTIFICATIONS, facts.notificationsAllowed))
    if (facts.isParent) add(StepState(FirstStep.PLAN_TRIP, facts.hasTrip))
}

/** Shown until everything is done or the person hides it for good. */
fun showFirstRun(steps: List<StepState>, hidden: Boolean): Boolean = !hidden && steps.any { !it.done }
