package com.marcm.cadencia.ui.all

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.data.repository.DomainRepository
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.TaskStatus
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.domain.recurrence.Schedule
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Una tarea del inventario con su estado respecto a hoy. */
data class AllTaskRow(
    val item: TaskWithContext,
    val status: TaskStatus,
    val daysOverdue: Long,
    val daysUntilDue: Long
) {
    val id: Long get() = item.id
}

/** Las tareas de un ámbito, ya ordenadas. */
data class DomainGroup(
    val domain: Domain,
    val rows: List<AllTaskRow>
)

data class AllTasksUiState(
    val groups: List<DomainGroup> = emptyList(),
    val domains: List<Domain> = emptyList(),
    val filterDomainId: Long? = null,
    val visibleCount: Int = 0,
    val overdueCount: Int = 0,
    val loaded: Boolean = false
) {
    val isEmpty: Boolean get() = groups.isEmpty()
}

/**
 * Inventario completo: todas las tareas activas, agrupadas por ámbito y sin filtrar por
 * fecha. Es la vista de "qué tengo montado", complementaria a Hoy (qué toca ahora) y al
 * Plan (qué viene en dos semanas).
 */
class AllTasksViewModel(
    repo: TaskRepository,
    domainRepo: DomainRepository
) : ViewModel() {

    private val filter = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AllTasksUiState> =
        combine(
            repo.observeActiveTasks(),
            domainRepo.observeActive(),
            filter
        ) { tasks, domains, activeFilter ->
            buildState(tasks, domains, activeFilter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllTasksUiState())

    private fun buildState(
        tasks: List<TaskWithContext>,
        domains: List<Domain>,
        activeFilter: Long?
    ): AllTasksUiState {
        val today = LocalDate.now()

        // Si el ámbito filtrado se desactiva, el filtro deja de tener sentido.
        val filterId = activeFilter?.takeIf { id -> domains.any { it.id == id } }

        val rows = tasks.map { item ->
            AllTaskRow(
                item = item,
                status = Schedule.status(item.task, today),
                daysOverdue = Schedule.daysOverdue(item.task, today),
                daysUntilDue = Schedule.daysUntilDue(item.task, today)
            )
        }
        val visible = if (filterId == null) rows else rows.filter { it.item.domain.id == filterId }

        // Un grupo por ámbito con tareas; dentro, lo más urgente primero y a igualdad de
        // fecha por nombre, que es el orden con el que ya llegan del DAO.
        val byDomain = visible.groupBy { it.item.domain.id }
        val groups = domains
            .sortedBy { it.sortOrder }
            .mapNotNull { domain ->
                val ofDomain = byDomain[domain.id].orEmpty()
                if (ofDomain.isEmpty()) null
                else DomainGroup(
                    domain = domain,
                    rows = ofDomain.sortedWith(
                        compareBy({ it.item.task.dueDate }, { it.item.task.name.lowercase() })
                    )
                )
            }

        return AllTasksUiState(
            groups = groups,
            domains = domains,
            filterDomainId = filterId,
            visibleCount = visible.size,
            overdueCount = visible.count { it.status == TaskStatus.OVERDUE },
            loaded = true
        )
    }

    fun setFilter(domainId: Long?) {
        filter.value = domainId
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                AllTasksViewModel(container.taskRepository, container.domainRepository)
            }
        }
    }
}
