package com.choreme.feature.chores

import com.choreme.core.chores.ChoreChange
import com.choreme.core.chores.ChoreChanger
import com.choreme.core.model.chore.without
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
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
import com.choreme.core.model.chore.ChoreChoice
import com.choreme.core.model.chore.choreChoices
import com.choreme.core.model.chore.guessTemplate
import com.choreme.core.model.chore.mostUsed
import com.choreme.core.model.chore.savedChoreToUpdate
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
import com.choreme.core.push.PushKinds
import com.choreme.core.push.PushSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.choreme.core.model.chore.RoomFilter
import com.choreme.core.model.chore.choreDone
import com.choreme.core.model.chore.filteredBy
import com.choreme.core.model.chore.nextReminderTime
import com.choreme.core.data.RoomRepository
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.YourTask
import com.choreme.core.model.room.yourTasks
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

internal data class ChoresUiState(
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
    /** Everything this person took, the house's chores and the tasks of the rooms together (the rooms' tagged). */
    val yours: List<YourTask> = emptyList(),
    /** Whether the family plays with points; off, they show nowhere. */
    val pointsOn: Boolean = true,
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

    /** What the editor offers: ready-made chores, then saved ones; and the family's most used. */
    val choices: List<ChoreChoice> get() = choreChoices(savedChores)
    val popular: List<ChoreChoice> get() = mostUsed(choices, chores)
}

/** The state without the chores being deleted (they wait a few seconds for Undo). */
internal fun ChoresUiState.without(ids: Set<String>): ChoresUiState = if (ids.isEmpty()) this else copy(
    board = board.without(ids),
    chores = chores.filter { it.id !in ids },
    yours = yours.filter { it.chore.id !in ids },
)

/** What the person typed in the chore editor. */
internal data class ChoreDraft(
    val title: String,
    val template: ChoreTemplate,
    val recurrence: Recurrence,
    val dueDate: LocalDate,
    val notifyWhenDone: Boolean = true,
    val urgent: Boolean = false,
    val points: Int = 1,
    /** Who it's for, picked by a parent: nobody means anyone can grab it. */
    val people: Set<String> = emptySet(),
    /** The last day it repeats (null: for ever); ignored for one-off chores. */
    val repeatUntil: LocalDate? = null,
)

/** What the Chores tab reads from the family's data, before the family's own settings are added. */
private data class Data(
    val chores: List<Chore>,
    val members: List<Member>,
    val groceryItems: List<com.choreme.core.model.grocery.GroceryItem>,
    val saved: List<SavedChore>,
    val rooms: List<Room>,
)

internal sealed interface CalendarResult {
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
    private val changer: ChoreChanger,
    rooms: RoomRepository,
) : ViewModel() {
    /** This phone's "remind me at" times, by chore id. */
    internal val reminders: StateFlow<Map<String, Instant>> = timedReminders.all

    /** Returns when it'll go off: the next [at], today or tomorrow. */
    internal fun remind(chore: Chore, at: LocalTime): LocalDateTime {
        val time = nextReminderTime(LocalDateTime.now(), at)
        timedReminders.set(chore.id, chore.title, time.atZone(ZoneId.systemDefault()).toInstant())
        return time
    }

    internal fun cancelReminder(chore: Chore) = timedReminders.cancel(chore.id)

    /** Chores deleted a moment ago: hidden at once, really deleted after a few seconds unless undone. */
    private val deleting = PendingDeletes(viewModelScope) { id -> uiState.value.familyId?.let { chores.delete(it, id) } }

    private val baseState: Flow<ChoresUiState> = families.inFamily
        .flatMapLatest { session ->
            combine(
                combine(
                    chores.observeChores(session.familyId),
                    families.observeMembers(session.familyId),
                    groceries.observeItems(session.familyId),
                    chores.observeSavedChores(session.familyId),
                    rooms.observe(session.familyId),
                    ::Data,
                ),
                families.observeFamily(session.familyId),
            ) { (allChores, members, groceryItems, saved, roomList), family ->
                val today = LocalDate.now()
                val house = buildTodayBoard(allChores.filteredBy(RoomFilter.NO_ROOM), today, session.memberId)
                ChoresUiState(
                    loading = false,
                    familyId = session.familyId,
                    me = members.firstOrNull { it.id == session.memberId }
                        ?: Member(session.memberId, name = "", role = session.role),
                    today = today,
                    // The tasks of a room live under the Rooms sub-tab, so this board is the house's own chores.
                    board = house,
                    yours = yourTasks(house.mine, buildTodayBoard(allChores.filteredBy(RoomFilter.ROOMS), today, session.memberId).mine, roomList),
                    chores = allChores,
                    members = members.associateBy { it.id },
                    groceriesToBuy = groceryItems.groceriesToBuy(),
                    savedChores = saved,
                    pointsOn = family?.pointsEnabled ?: true,
                )
            }
        }

    internal val uiState: StateFlow<ChoresUiState> = combine(baseState, deleting.ids) { state, hidden -> state.without(hidden) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChoresUiState())

    internal fun claim(chore: Chore) = change(chore) { me, _ -> claim(me) }
    internal fun unclaim(chore: Chore) = change(chore) { me, _ -> unclaim(me) }
    internal fun complete(chore: Chore) = change(chore) { me, today -> complete(me, today) }
    internal fun join(chore: Chore) = change(chore) { me, _ -> join(me) }
    internal fun assign(chore: Chore, people: Set<String>) = change(chore) { me, _ -> assign(people, me) }
    internal fun completeFor(chore: Chore, people: Set<String>) = change(chore) { me, today -> completeFor(people, me, today) }
    internal fun approve(chore: Chore) = change(chore) { me, today -> approve(me, today) }
    internal fun reject(chore: Chore) = change(chore) { me, _ -> reject(me) }

    /** Adds the chore unless the same thing is already waiting to be done; returns that one if so. */
    internal fun create(draft: ChoreDraft): Chore? {
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
            repeatUntil = draft.repeatUntil.takeIf { draft.recurrence != Recurrence.Once },
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
    internal suspend fun addToCalendar(chore: Chore): CalendarResult {
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

    internal fun removeSaved(saved: SavedChore) {
        val (familyId, _) = context() ?: return
        chores.deleteSaved(familyId, saved.id)
    }



    internal fun update(chore: Chore, draft: ChoreDraft) = change(chore) { me, _ ->
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
            repeatUntil = draft.repeatUntil.takeIf { draft.recurrence != Recurrence.Once },
            dueDate = draft.dueDate,
            notifyWhenDone = draft.notifyWhenDone,
            urgent = draft.urgent,
        )
    }

    internal fun delete(chore: Chore) {
        if (context() != null) deleting.delete(chore.id)
    }

    internal fun undoDelete(chore: Chore) = deleting.undo(chore.id)

    internal fun undo(change: ChoreChange) {
        val (familyId, me) = context() ?: return
        changer.undo(familyId, me, change)
    }

    /** Leaving the tab doesn't keep a chore that was deleted: the waiting deletes happen now. */
    override fun onCleared() {
        deleting.flush()
        super.onCleared()
    }

    /** Applies one of the chore rules from :core:model (see [ChoreChanger]); what changed, for Undo. */
    private fun change(chore: Chore, transition: Chore.(me: Member, today: LocalDate) -> Chore): ChoreChange? {
        val (familyId, me) = context() ?: return null
        val parents = uiState.value.members.values.filter { it.role == Role.PARENT }.map { it.id }.toSet()
        return changer.change(viewModelScope, familyId, me, parents, chore, transition)
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
