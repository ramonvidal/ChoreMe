package com.choreme.feature.chores

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.calendar.CalendarChoice
import com.choreme.core.calendar.CalendarRepository
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.GroceryRepository
import com.choreme.core.data.PushEndpointRepository
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.agenda.NewEvent
import com.choreme.core.model.agenda.calendarForChores
import com.choreme.core.model.grocery.groceriesToBuy
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreStatus
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.SavedChore
import com.choreme.core.model.chore.HomeScene
import com.choreme.core.model.chore.ChoreChoice
import com.choreme.core.model.chore.choreChoices
import com.choreme.core.model.chore.guessTemplate
import com.choreme.core.model.chore.mostUsed
import com.choreme.core.model.chore.MemberWeek
import com.choreme.core.model.chore.homeScene
import com.choreme.core.model.chore.savedChoreToUpdate
import com.choreme.core.model.chore.weekStart
import com.choreme.core.model.chore.weeklySummary
import com.choreme.core.model.chore.familyPoints
import com.choreme.core.model.chore.join
import com.choreme.core.model.chore.assign
import com.choreme.core.model.chore.TodayBoard
import com.choreme.core.model.chore.approve
import com.choreme.core.model.chore.buildTodayBoard
import com.choreme.core.model.chore.claim
import com.choreme.core.model.chore.complete
import com.choreme.core.model.chore.findOpenDuplicate
import com.choreme.core.model.chore.completeFor
import com.choreme.core.model.chore.reject
import com.choreme.core.model.chore.shouldSaveForLater
import com.choreme.core.model.chore.unclaim
import com.choreme.core.model.chore.wasJustFinished
import com.choreme.core.model.session.SessionState
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.choreme.core.model.chore.choreDone
import com.choreme.core.model.chore.nextReminderTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class ChoresUiState(
    val loading: Boolean = true,
    val familyId: String? = null,
    val me: Member? = null,
    val today: LocalDate = LocalDate.now(),
    val board: TodayBoard = TodayBoard(emptyList(), emptyList(), emptyList(), emptyList()),
    val chores: List<Chore> = emptyList(),
    val members: Map<String, Member> = emptyMap(),
    val groceriesToBuy: Int = 0,
    /** Hand-typed chores kept as quick picks. */
    val savedChores: List<SavedChore> = emptyList(),
    /** Everyone's finishes in the last 7 days, for the garden. */
    val week: List<MemberWeek> = emptyList(),
    /** The family's points in the last 7 days; a chore done together counts once. */
    val weekPoints: Int = 0,
) {
    val isParent get() = me?.role == Role.PARENT
    val hasChores get() = chores.isNotEmpty()
    /** Everyone at home, for "who's doing it" and "who did it" (someone away can't have). */
    val atHome get() = members.values.filter { !it.isAway(today) }.sortedBy { it.name.lowercase() }

    /** "Leo", "Leo and Mia", "Leo, Mia and Ana": everyone on a chore but [me], whoever grabbed it first. */
    fun teamNames(chore: Chore, me: String?): List<String> =
        (listOfNotNull(chore.claimedBy) + chore.helpers.sorted()).filter { it != me }.mapNotNull { members[it]?.name }
    fun nameOf(memberId: String?): String? = memberId?.let { members[it]?.name }

    /** Parents tidy up any saved chore; others only their own. */
    fun canRemove(saved: SavedChore) = isParent || saved.createdBy == me?.id

    val scene: HomeScene get() = homeScene(weekPoints, chores, today)

    /** What the editor offers: ready-made chores, then saved ones; and the family's most used. */
    val choices: List<ChoreChoice> get() = choreChoices(savedChores)
    val popular: List<ChoreChoice> get() = mostUsed(choices, chores)
}

/** What the person typed in the chore editor. */
data class ChoreDraft(
    val title: String,
    val template: ChoreTemplate,
    val recurrence: Recurrence,
    val dueDate: LocalDate,
    val notifyWhenDone: Boolean = true,
    val urgent: Boolean = false,
    val points: Int = 1,
    /** Who it's for, picked by a parent: nobody means anyone can grab it. */
    val people: Set<String> = emptySet(),
)

sealed interface CalendarResult {
    data class Added(val calendarName: String) : CalendarResult
    data class OpenApp(val intent: Intent) : CalendarResult
}

@HiltViewModel
class ChoresViewModel @Inject internal constructor(
    families: FamilyRepository,
    private val chores: ChoreRepository,
    groceries: GroceryRepository,
    private val endpoints: PushEndpointRepository,
    private val push: PushSender,
    private val calendar: CalendarRepository,
    private val calendarChoice: CalendarChoice,
    private val timedReminders: TimedReminders,
) : ViewModel() {
    /** This phone's "remind me at" times, by chore id. */
    val reminders: StateFlow<Map<String, Instant>> = timedReminders.all

    /** Returns when it'll go off: the next [at], today or tomorrow. */
    fun remind(chore: Chore, at: LocalTime): LocalDateTime {
        val time = nextReminderTime(LocalDateTime.now(), at)
        timedReminders.set(chore.id, chore.title, time.atZone(ZoneId.systemDefault()).toInstant())
        return time
    }

    fun cancelReminder(chore: Chore) = timedReminders.cancel(chore.id)

    val uiState: StateFlow<ChoresUiState> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .flatMapLatest { session ->
            combine(
                chores.observeChores(session.familyId),
                families.observeMembers(session.familyId),
                groceries.observeItems(session.familyId),
                chores.observeSavedChores(session.familyId),
                chores.observeFinished(session.familyId, weekStart(LocalDate.now())).onStart { emit(emptyList()) },
            ) { allChores, members, groceryItems, saved, finished ->
                val today = LocalDate.now()
                ChoresUiState(
                    loading = false,
                    familyId = session.familyId,
                    me = members.firstOrNull { it.id == session.memberId }
                        ?: Member(session.memberId, name = "", role = session.role),
                    today = today,
                    board = buildTodayBoard(allChores, today, session.memberId),
                    chores = allChores,
                    members = members.associateBy { it.id },
                    groceriesToBuy = groceryItems.groceriesToBuy(),
                    savedChores = saved,
                    week = weeklySummary(finished, members, today),
                    weekPoints = familyPoints(finished, today),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChoresUiState())

    fun claim(chore: Chore) = change(chore) { me, _ -> claim(me) }
    fun unclaim(chore: Chore) = change(chore) { me, _ -> unclaim(me) }
    fun complete(chore: Chore) = change(chore) { me, today -> complete(me, today) }
    fun join(chore: Chore) = change(chore) { me, _ -> join(me) }
    fun assign(chore: Chore, people: Set<String>) = change(chore) { me, _ -> assign(people, me) }
    fun completeFor(chore: Chore, people: Set<String>) = change(chore) { me, today -> completeFor(people, me, today) }
    fun approve(chore: Chore) = change(chore) { me, today -> approve(me, today) }
    fun reject(chore: Chore) = change(chore) { me, _ -> reject(me) }

    /** Adds the chore unless the same thing is already waiting to be done; returns that one if so. */
    fun create(draft: ChoreDraft): Chore? {
        findOpenDuplicate(draft.title, uiState.value.chores)?.let { existing ->
            // "It's already on the list, and now it's urgent": parents can raise it.
            if (draft.urgent && !existing.urgent && uiState.value.isParent) change(existing) { _, _ -> copy(urgent = true) }
            // ...and still free: then it goes to whoever the parent picked.
            if (draft.people.isNotEmpty() && existing.status == ChoreStatus.OPEN && uiState.value.isParent) {
                change(existing) { me, _ -> assign(draft.people, me) }
            }
            return existing
        }
        val (familyId, me) = context() ?: return null
        // Kids report at the starting worth; a parent can raise it (see firestore.rules).
        val points = if (me.role == Role.PARENT) draft.points.coerceIn(Chore.POINTS) else Chore.POINTS.first
        // A typed chore gets the icon of the ready-made one it's closest to ("Lixo Bio": a leaf).
        val template = if (draft.template == ChoreTemplate.OTHER) guessTemplate(draft.title) ?: ChoreTemplate.OTHER else draft.template
        val chore = Chore(
            id = "",
            title = draft.title.trim(),
            template = template,
            recurrence = draft.recurrence,
            dueDate = draft.dueDate,
            notifyWhenDone = draft.notifyWhenDone,
            urgent = draft.urgent,
            points = points,
        )
        // A parent can give it to someone (or several) straight away; kids report it for anyone.
        val people = draft.people.filterTo(mutableSetOf()) { it in uiState.value.members }
        chores.create(familyId, me.id, if (me.role == Role.PARENT && people.isNotEmpty()) chore.assign(people, me) else chore)
        // Something typed by hand becomes a quick pick for next time.
        if (shouldSaveForLater(draft.template, draft.title, uiState.value.savedChores)) {
            chores.saveForLater(familyId, me.id, draft.title, points, template)
        } else if (me.role == Role.PARENT) {
            // A saved chore picked with other points keeps them for next time.
            savedChoreToUpdate(draft.title, points, uiState.value.savedChores)?.let { chores.setSavedPoints(familyId, it.id, it.points) }
        }
        // Let the rest of the family know right away (phones with ntfy).
        pingInBackground(PushKinds.CHORE_ADDED) { endpoints.othersThan(familyId, me.id) }
        return null
    }

    /**
     * Puts the chore on its due day in the family calendar, so it shows in everyone's Google
     * Calendar. Goes straight in when the agenda shows exactly one calendar this phone can
     * write to; otherwise the calendar app opens, filled in, to pick one.
     */
    suspend fun addToCalendar(chore: Chore): CalendarResult {
        val event = NewEvent(title = chore.title, date = chore.dueDate, startTime = null)
        val target = if (calendar.hasPermission()) {
            calendarForChores(calendar.observeCalendars().first(), calendarChoice.selected.value)
        } else {
            null
        }
        return if (target == null) {
            CalendarResult.OpenApp(calendar.insertIntent(event))
        } else {
            try {
                calendar.add(target.id, event)
                CalendarResult.Added(target.name)
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't add the chore to the calendar", e)
                CalendarResult.OpenApp(calendar.insertIntent(event))
            }
        }
    }

    fun removeSaved(saved: SavedChore) {
        val (familyId, _) = context() ?: return
        chores.deleteSaved(familyId, saved.id)
    }



    fun update(chore: Chore, draft: ChoreDraft) = change(chore) { me, _ ->
        // Who it's for changes only when a parent picked differently, and only while it's to do.
        // Saving the same people again also makes it last (chores given out before assignments stayed).
        val reassigned = if (me.role == Role.PARENT && isActive && (draft.people != team || draft.people != assignedTo)) {
            assign(draft.people, me)
        } else {
            this
        }
        reassigned.copy(
            points = if (me.role == Role.PARENT) draft.points.coerceIn(Chore.POINTS) else points,
            title = draft.title.trim(),
            template = draft.template,
            recurrence = draft.recurrence,
            dueDate = draft.dueDate,
            notifyWhenDone = draft.notifyWhenDone,
            urgent = draft.urgent,
        )
    }

    fun delete(chore: Chore) {
        val (familyId, _) = context() ?: return
        chores.delete(familyId, chore.id)
    }

    /**
     * Applies one of the chore rules from :core:model and saves the result. A rule that
     * refuses (say, someone grabbed the chore a moment ago) leaves the chore as it is.
     */
    private fun change(chore: Chore, transition: Chore.(me: Member, today: LocalDate) -> Chore) {
        val (familyId, me) = context() ?: return
        val updated = try {
            chore.transition(me, LocalDate.now())
        } catch (e: IllegalStateException) {
            Log.i("ChoreMe", "Chore change refused: ${e.message}")
            return
        } catch (e: IllegalArgumentException) {
            Log.i("ChoreMe", "Chore change refused: ${e.message}")
            return
        }
        chores.save(familyId, me.id, chore, updated)
        // For "who did what this week"; finishing is always a parent's step (kids' chores get approved).
        if (me.role == Role.PARENT) choreDone(chore, updated)?.let { chores.logDone(familyId, it) }
        if (updated.notifyWhenDone && updated.wasJustFinished(chore)) {
            pingInBackground(PushKinds.CHORE_DONE) { endpoints.othersThan(familyId, me.id) }
        }
        if (updated.status == ChoreStatus.PENDING_APPROVAL && chore.status != ChoreStatus.PENDING_APPROVAL) {
            val parents = uiState.value.members.values.filter { it.role == Role.PARENT }.map { it.id }.toSet()
            pingInBackground(PushKinds.CHORE_TO_APPROVE) { endpoints.of(familyId, parents) }
        }
    }

    private fun pingInBackground(kind: String, to: suspend () -> List<String>) {
        viewModelScope.launch {
            try {
                push.ping(to(), kind)
            } catch (e: Exception) {
                Log.w("ChoreMe", "Couldn't ping the family", e)
            }
        }
    }

    private fun context(): Pair<String, Member>? {
        val state = uiState.value
        return (state.familyId ?: return null) to (state.me ?: return null)
    }
}
