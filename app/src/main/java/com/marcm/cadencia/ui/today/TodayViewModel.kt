package com.marcm.cadencia.ui.today

import android.content.Context
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
import com.marcm.cadencia.notifications.NotificationHelper
import com.marcm.cadencia.ui.appContainer
import com.marcm.cadencia.ui.components.DomainProgress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/** Una tarea con lo que la pantalla necesita saber de ella hoy. */
data class TodayRow(
    val item: TaskWithContext,
    val status: TaskStatus,
    val daysOverdue: Long
) {
    val id: Long get() = item.id
}

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val overdue: List<TodayRow> = emptyList(),
    val today: List<TodayRow> = emptyList(),
    val doneCount: Int = 0,
    val total: Int = 0,
    val domains: List<Domain> = emptyList(),
    val domainProgress: List<DomainProgress> = emptyList(),
    val filterDomainId: Long? = null,
    val loaded: Boolean = false
) {
    val pct: Int get() = if (total == 0) 0 else (doneCount * 100) / total
    val isEmpty: Boolean get() = overdue.isEmpty() && today.isEmpty()
}

class TodayViewModel(
    private val repo: TaskRepository,
    domainRepo: DomainRepository,
    private val appContext: Context
) : ViewModel() {

    private val filter = MutableStateFlow<Long?>(null)

    /** Se emite al completar la última pendiente del día (celebración). */
    private val _celebrate = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val celebrate: SharedFlow<Unit> = _celebrate.asSharedFlow()

    val uiState: StateFlow<TodayUiState> =
        combine(
            repo.observeActiveTasks(),
            domainRepo.observeActive(),
            filter
        ) { tasks, domains, activeFilter ->
            buildState(tasks, domains, activeFilter)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    private fun buildState(
        tasks: List<TaskWithContext>,
        domains: List<Domain>,
        activeFilter: Long?
    ): TodayUiState {
        val today = LocalDate.now()

        // Si el ámbito filtrado se desactiva, el filtro deja de tener sentido.
        val filterId = activeFilter?.takeIf { id -> domains.any { it.id == id } }

        val rows = tasks.map { item ->
            TodayRow(
                item = item,
                status = Schedule.status(item.task, today),
                daysOverdue = Schedule.daysOverdue(item.task, today)
            )
        }

        // "En juego hoy" = vencidas, de hoy y las ya hechas hoy. Lo futuro no cuenta.
        val inPlay = rows.filter { it.status != TaskStatus.FUTURE }
        val visible = if (filterId == null) inPlay
        else inPlay.filter { it.item.domain.id == filterId }

        val progressDomains = if (filterId == null) domains else domains.filter { it.id == filterId }
        val perDomain = progressDomains.map { domain ->
            val ofDomain = inPlay.filter { it.item.domain.id == domain.id }
            DomainProgress(
                domain = domain,
                done = ofDomain.count { it.status == TaskStatus.DONE },
                total = ofDomain.size
            )
        }.filter { it.total > 0 }

        return TodayUiState(
            date = today,
            overdue = visible.filter { it.status == TaskStatus.OVERDUE }
                .sortedByDescending { it.daysOverdue },
            // Las hechas se quedan donde estaban, atenuadas y tachadas: si saltaran al
            // final, el gesto de marcar haría desaparecer la fila que acabas de tocar.
            today = visible.filter { it.status == TaskStatus.DUE_TODAY || it.status == TaskStatus.DONE }
                .sortedBy { it.item.task.name.lowercase() },
            doneCount = visible.count { it.status == TaskStatus.DONE },
            total = visible.size,
            domains = domains,
            domainProgress = perDomain,
            filterDomainId = filterId,
            loaded = true
        )
    }

    fun setFilter(domainId: Long?) {
        filter.value = domainId
    }

    fun greeting(): String = com.marcm.cadencia.ui.components.greeting(LocalTime.now())

    fun toggle(row: TodayRow) {
        viewModelScope.launch {
            if (row.status == TaskStatus.DONE) {
                repo.undoComplete(row.item.task)
            } else {
                val before = uiState.value
                val wasLast = before.total > 0 && before.doneCount + 1 == before.total
                repo.complete(row.item.task)
                if (wasLast) {
                    _celebrate.tryEmit(Unit)
                    NotificationHelper.showCelebration(appContext)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = this.appContainer()
                TodayViewModel(
                    container.taskRepository,
                    container.domainRepository,
                    container.appContext
                )
            }
        }
    }
}
