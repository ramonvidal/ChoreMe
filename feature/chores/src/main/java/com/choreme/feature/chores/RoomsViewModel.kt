package com.choreme.feature.chores

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.choreme.core.data.ChoreRepository
import com.choreme.core.data.FamilyRepository
import com.choreme.core.data.RoomRepository
import com.choreme.core.model.Member
import com.choreme.core.model.Role
import com.choreme.core.model.chore.Chore
import com.choreme.core.model.chore.ChoreTemplate
import com.choreme.core.model.chore.Recurrence
import com.choreme.core.model.chore.complete
import com.choreme.core.model.chore.reject
import com.choreme.core.model.chore.approve
import com.choreme.core.model.chore.assign
import com.choreme.core.model.chore.join
import com.choreme.core.model.chore.unclaim
import com.choreme.core.model.chore.claim
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomOverview
import com.choreme.core.model.room.newRoomTask
import com.choreme.core.model.room.roomOverviews
import com.choreme.core.model.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

internal data class RoomsUiState(
    val loading: Boolean = true,
    val rooms: List<RoomOverview> = emptyList(),
    /** Parents add, change and remove rooms and their tasks; everyone can say a task is done. */
    val canManage: Boolean = false,
    val today: LocalDate = LocalDate.now(),
    /** Who is looking, and the whole family by id (names for "Leo is on it", people to give a task to). */
    val me: Member? = null,
    val members: Map<String, Member> = emptyMap(),
) {
    /** Everyone who is home, for "who's doing it" (someone away can't). */
    val atHome get() = members.values.filter { !it.isAway(today) }.sortedBy { it.name.lowercase() }
}

@HiltViewModel
class RoomsViewModel @Inject internal constructor(
    private val families: FamilyRepository,
    private val roomsRepository: RoomRepository,
    private val chores: ChoreRepository,
    private val changer: ChoreChanger,
) : ViewModel() {
    private val session: StateFlow<SessionState.InFamily?> = families.inFamily
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val familySession = families.inFamily

    private val members = familySession.flatMapLatest { families.observeMembers(it.familyId) }

    /** Tasks deleted a moment ago: hidden at once, really deleted after a few seconds unless undone. */
    private val deleting = PendingDeletes(viewModelScope) { id -> session.value?.let { chores.delete(it.familyId, id) } }

    internal val uiState: StateFlow<RoomsUiState> = combine(
        familySession,
        familySession.flatMapLatest { roomsRepository.observe(it.familyId) },
        familySession.flatMapLatest { chores.observeChores(it.familyId) },
        members,
        deleting.ids,
    ) { s, rooms, allChores, memberList, hidden ->
        RoomsUiState(
            loading = false,
            rooms = roomOverviews(rooms, allChores.filter { it.id !in hidden }),
            canManage = s.role == Role.PARENT,
            today = LocalDate.now(),
            me = memberList.firstOrNull { it.id == s.memberId } ?: Member(s.memberId, name = "", role = s.role),
            members = memberList.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoomsUiState())

    private val membersNow: StateFlow<List<Member>> = members.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Adds a room and returns its id, so its screen can open. Null when not in a family. */
    internal fun addRoom(room: Room): String? {
        val s = session.value ?: return null
        if (s.role != Role.PARENT) return null
        return roomsRepository.add(s.familyId, room)
    }

    internal fun saveRoom(room: Room) {
        val s = session.value ?: return
        if (s.role == Role.PARENT) roomsRepository.update(s.familyId, room)
    }

    /** Removes the room and the tasks that looked after it. */
    internal fun deleteRoom(overview: RoomOverview) {
        val s = session.value ?: return
        if (s.role != Role.PARENT) return
        overview.tasks.forEach { chores.delete(s.familyId, it.id) }
        roomsRepository.delete(s.familyId, overview.room.id)
    }

    /** A new repeating task for the room; [lastDoneDaysAgo] null when nobody remembers. */
    internal fun addTask(roomId: String, title: String, template: ChoreTemplate, everyDays: Int, lastDoneDaysAgo: Int?) {
        val s = session.value ?: return
        if (s.role != Role.PARENT || title.isBlank()) return
        val today = LocalDate.now()
        val task = newRoomTask(
            roomId = roomId,
            title = title,
            template = template,
            everyDays = everyDays,
            lastDoneOn = lastDoneDaysAgo?.let { today.minusDays(it.toLong()) },
            today = today,
        )
        chores.create(s.familyId, s.memberId, task)
    }

    /** Changes how often a task comes round; the next date follows from when it was last done. */
    internal fun setFrequency(task: Chore, everyDays: Int) {
        val s = session.value ?: return
        if (s.role != Role.PARENT || task.recurrence == Recurrence.EveryDays(everyDays)) return
        val due = task.lastDoneOn?.plusDays(everyDays.toLong()) ?: task.dueDate
        chores.save(s.familyId, s.memberId, task, task.copy(recurrence = Recurrence.EveryDays(everyDays), dueDate = due))
    }

    internal fun deleteTask(task: Chore) {
        val s = session.value ?: return
        if (s.role == Role.PARENT) deleting.delete(task.id)
    }

    internal fun undoDeleteTask(task: Chore) = deleting.undo(task.id)

    internal fun undo(change: ChoreChange) {
        val s = session.value ?: return
        val me = membersNow.value.firstOrNull { it.id == s.memberId } ?: return
        changer.undo(s.familyId, me, change)
    }

    /** Leaving the tab doesn't keep a task that was deleted: the waiting deletes happen now. */
    override fun onCleared() {
        deleting.flush()
        super.onCleared()
    }

    /** Applies one of the chore rules to a task of a room; the same rules and alerts as in the Chores list. */
    private fun change(task: Chore, transition: Chore.(me: Member, today: LocalDate) -> Chore): ChoreChange? {
        val s = session.value ?: return null
        val me = membersNow.value.firstOrNull { it.id == s.memberId } ?: return null
        val parents = membersNow.value.filter { it.role == Role.PARENT }.map { it.id }.toSet()
        return changer.change(viewModelScope, s.familyId, me, parents, task, transition)
    }

    /** "I did it": a parent's counts straight away, a kid's waits for a parent (see the chore rules). */
    internal fun markDone(task: Chore) = change(task) { me, today -> complete(me, today) }

    internal fun claim(task: Chore) = change(task) { me, _ -> claim(me) }
    internal fun unclaim(task: Chore) = change(task) { me, _ -> unclaim(me) }
    internal fun join(task: Chore) = change(task) { me, _ -> join(me) }
    internal fun assign(task: Chore, people: Set<String>) = change(task) { me, _ -> assign(people, me) }
    internal fun approve(task: Chore) = change(task) { me, today -> approve(me, today) }
    internal fun reject(task: Chore) = change(task) { me, _ -> reject(me) }
}
