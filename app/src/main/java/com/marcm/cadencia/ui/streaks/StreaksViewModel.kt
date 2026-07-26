package com.marcm.cadencia.ui.streaks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.data.local.CompletionDao
import com.marcm.cadencia.data.local.toDomain
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.domain.model.Completion
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.domain.recurrence.Schedule
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Constancia de una tarea concreta. */
data class TaskStreak(
    val item: TaskWithContext,
    val streak: Int,
    val longest: Int,
    val onTimeRate: Int?
)

/** Constancia agregada de un ámbito. */
data class DomainStreak(
    val domain: Domain,
    val tasks: List<TaskStreak>
) {
    val bestStreak: Int get() = tasks.maxOfOrNull { it.streak } ?: 0
    val onTimeRate: Int?
        get() = tasks.mapNotNull { it.onTimeRate }.takeIf { it.isNotEmpty() }?.average()?.toInt()
}

data class StreaksUiState(
    val domains: List<DomainStreak> = emptyList(),
    val loaded: Boolean = false
) {
    val hasHistory: Boolean get() = domains.any { d -> d.tasks.any { it.onTimeRate != null } }
}

/**
 * Vista agregada de constancia. Lee todos los completados de una vez y los reparte por
 * tarea, en lugar de consultar tarea a tarea.
 */
class StreaksViewModel(
    repo: TaskRepository,
    completionDao: CompletionDao
) : ViewModel() {

    val uiState: StateFlow<StreaksUiState> =
        combine(
            repo.observeActiveTasks(),
            completionDao.observeAll().map { list -> list.map { it.toDomain() } }
        ) { tasks, completions ->
            buildState(tasks, completions)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreaksUiState())

    private fun buildState(
        tasks: List<TaskWithContext>,
        completions: List<Completion>
    ): StreaksUiState {
        val byTask = completions.groupBy { it.taskId }

        val domains = tasks
            .groupBy { it.domain.id }
            .map { (_, items) ->
                DomainStreak(
                    domain = items.first().domain,
                    tasks = items.map { item ->
                        val own = byTask[item.id].orEmpty()
                        TaskStreak(
                            item = item,
                            streak = Schedule.currentStreak(own),
                            longest = Schedule.longestStreak(own),
                            onTimeRate = Schedule.onTimeRate(own)
                        )
                    }.sortedByDescending { it.streak }
                )
            }
            .sortedBy { it.domain.sortOrder }

        return StreaksUiState(domains = domains, loaded = true)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = appContainer()
                StreaksViewModel(container.taskRepository, container.completionDao)
            }
        }
    }
}
