package com.marcm.cadencia.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.domain.model.Completion
import com.marcm.cadencia.domain.model.TaskStatus
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.domain.recurrence.Schedule
import com.marcm.cadencia.notifications.ReminderScheduler
import com.marcm.cadencia.ui.appContainer
import com.marcm.cadencia.ui.navigation.Routes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Cómo salió un ciclo: a tiempo, con retraso, o el que está en curso. */
enum class CycleOutcome { ON_TIME, LATE, PENDING }

data class CycleBar(val outcome: CycleOutcome, val date: LocalDate)

data class DetailUiState(
    val item: TaskWithContext? = null,
    val status: TaskStatus = TaskStatus.FUTURE,
    val daysOverdue: Long = 0,
    val streak: Int = 0,
    val longestStreak: Int = 0,
    val onTimeRate: Int? = null,
    val cycles: List<CycleBar> = emptyList(),
    val history: List<Completion> = emptyList(),
    val today: LocalDate = LocalDate.now()
) {
    val isDoneToday: Boolean get() = status == TaskStatus.DONE
}

class TaskDetailViewModel(
    private val repo: TaskRepository,
    private val scheduler: ReminderScheduler,
    private val taskId: Long,
    private val zone: ZoneId = ZoneId.systemDefault()
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> =
        combine(repo.observeTask(taskId), repo.observeCompletions(taskId)) { item, completions ->
            buildState(item, completions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    private fun buildState(item: TaskWithContext?, completions: List<Completion>): DetailUiState {
        val today = LocalDate.now()
        if (item == null) return DetailUiState(today = today)

        val status = Schedule.status(item.task, today)

        // Últimos ciclos cerrados (más antiguos primero) más el ciclo en curso.
        val closed = completions
            .sortedBy { it.completedAt }
            .takeLast(CYCLES - 1)
            .map {
                CycleBar(
                    outcome = if (it.onTime) CycleOutcome.ON_TIME else CycleOutcome.LATE,
                    date = it.completedAt.atZone(zone).toLocalDate()
                )
            }
        val cycles = closed + CycleBar(CycleOutcome.PENDING, item.task.dueDate)

        return DetailUiState(
            item = item,
            status = status,
            daysOverdue = Schedule.daysOverdue(item.task, today),
            streak = Schedule.currentStreak(completions),
            longestStreak = Schedule.longestStreak(completions),
            onTimeRate = Schedule.onTimeRate(completions),
            cycles = cycles,
            history = completions,
            today = today
        )
    }

    fun toggleToday() {
        val current = uiState.value.item ?: return
        viewModelScope.launch {
            val updated = if (uiState.value.isDoneToday) repo.undoComplete(current.task)
            else repo.complete(current.task)
            scheduler.schedule(updated)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = uiState.value.item ?: return
        viewModelScope.launch {
            scheduler.cancel(current.task)
            repo.delete(current.task)
            onDeleted()
        }
    }

    companion object {
        private const val CYCLES = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                val handle: SavedStateHandle = createSavedStateHandle()
                val id = handle.get<String>(Routes.ARG_TASK_ID)?.toLongOrNull() ?: -1L
                TaskDetailViewModel(container.taskRepository, container.reminderScheduler, id)
            }
        }
    }
}
