package com.choreme.feature.chores

import android.util.Log
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
import com.choreme.core.model.chore.choreDone
import com.choreme.core.model.chore.complete
import com.choreme.core.model.room.Room
import com.choreme.core.model.room.RoomOverview
import com.choreme.core.model.room.newRoomTask
import com.choreme.core.model.room.roomOverviews
import com.choreme.core.model.session.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class RoomsUiState(
    val loading: Boolean = true,
    val rooms: List<RoomOverview> = emptyList(),
    /** Parents add, change and remove rooms and their tasks; everyone can say a task is done. */
    val canManage: Boolean = false,
    val today: LocalDate = LocalDate.now(),
)

@HiltViewModel
class RoomsViewModel @Inject constructor(
    private val families: FamilyRepository,
    private val roomsRepository: RoomRepository,
    private val chores: ChoreRepository,
) : ViewModel() {
    private val session: StateFlow<SessionState.InFamily?> = families.session
        .filterIsInstance<SessionState.InFamily>()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val familySession = families.session.filterIsInstance<SessionState.InFamily>()

    private val members = familySession.flatMapLatest { families.observeMembers(it.familyId) }

    val uiState: StateFlow<RoomsUiState> = combine(
        familySession,
        familySession.flatMapLatest { roomsRepository.observe(it.familyId) },
        familySession.flatMapLatest { chores.observeChores(it.familyId) },
    ) { s, rooms, allChores ->
        RoomsUiState(
            loading = false,
            rooms = roomOverviews(rooms, allChores),
            canManage = s.role == Role.PARENT,
            today = LocalDate.now(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoomsUiState())

    private val membersNow: StateFlow<List<Member>> = members.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Adds a room and returns its id, so its screen can open. Null when not in a family. */
    fun addRoom(room: Room): String? {
        val s = session.value ?: return null
        if (s.role != Role.PARENT) return null
        return roomsRepository.add(s.familyId, room)
    }

    fun saveRoom(room: Room) {
        val s = session.value ?: return
        if (s.role == Role.PARENT) roomsRepository.update(s.familyId, room)
    }

    /** Removes the room and the tasks that looked after it. */
    fun deleteRoom(overview: RoomOverview) {
        val s = session.value ?: return
        if (s.role != Role.PARENT) return
        overview.tasks.forEach { chores.delete(s.familyId, it.id) }
        roomsRepository.delete(s.familyId, overview.room.id)
    }

    /** A new repeating task for the room; [lastDoneDaysAgo] null when nobody remembers. */
    fun addTask(roomId: String, title: String, template: ChoreTemplate, everyDays: Int, lastDoneDaysAgo: Int?) {
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
    fun setFrequency(task: Chore, everyDays: Int) {
        val s = session.value ?: return
        if (s.role != Role.PARENT || task.recurrence == Recurrence.EveryDays(everyDays)) return
        val due = task.lastDoneOn?.plusDays(everyDays.toLong()) ?: task.dueDate
        chores.save(s.familyId, s.memberId, task, task.copy(recurrence = Recurrence.EveryDays(everyDays), dueDate = due))
    }

    fun deleteTask(task: Chore) {
        val s = session.value ?: return
        if (s.role == Role.PARENT) chores.delete(s.familyId, task.id)
    }

    /** "I did it": a parent's counts straight away, a kid's waits for a parent (see the chore rules). */
    fun markDone(task: Chore) {
        val s = session.value ?: return
        val me = membersNow.value.firstOrNull { it.id == s.memberId } ?: return
        val updated = try {
            task.complete(me, LocalDate.now())
        } catch (e: IllegalStateException) {
            Log.i("ChoreMe", "Task change refused: ${e.message}")
            return
        } catch (e: IllegalArgumentException) {
            Log.i("ChoreMe", "Task change refused: ${e.message}")
            return
        }
        chores.save(s.familyId, s.memberId, task, updated)
        if (me.role == Role.PARENT) choreDone(task, updated)?.let { chores.logDone(s.familyId, it) }
    }
}
