package com.marcm.cadencia.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.marcm.cadencia.data.repository.TaskRepository
import com.marcm.cadencia.domain.model.Domain
import com.marcm.cadencia.domain.model.RecurrenceType
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.domain.recurrence.Schedule
import com.marcm.cadencia.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

/** Un día de la tira superior: qué ámbitos tienen algo ese día. */
data class StripDay(
    val date: LocalDate,
    val isToday: Boolean,
    val domains: List<Domain>
)

/** Una jornada del plan, con sus tareas no diarias. */
data class PlanDay(
    val date: LocalDate,
    val tasks: List<TaskWithContext>
)

data class PlanUiState(
    val today: LocalDate = LocalDate.now(),
    val strip: List<StripDay> = emptyList(),
    val days: List<PlanDay> = emptyList(),
    val dailyCount: Int = 0,
    val loaded: Boolean = false
)

/**
 * Plan a 14 días. Las tareas diarias no se listan jornada a jornada (llenarían la
 * pantalla de ruido): se cuentan aparte y se muestran colapsadas.
 */
class PlanViewModel(repo: TaskRepository) : ViewModel() {

    val uiState: StateFlow<PlanUiState> =
        repo.observeActiveTasks().map { tasks -> buildState(tasks) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    private fun buildState(tasks: List<TaskWithContext>): PlanUiState {
        val today = LocalDate.now()
        val until = today.plusDays(DAYS_AHEAD)

        val daily = tasks.filter { it.task.recurrence.type == RecurrenceType.DAILY }
        val rest = tasks.filter { it.task.recurrence.type != RecurrenceType.DAILY }

        // Ocurrencias de las no diarias, de hoy hasta el horizonte.
        val byDate = sortedMapOf<LocalDate, MutableList<TaskWithContext>>()
        rest.forEach { item ->
            Schedule.occurrencesInRange(item.task, today, until).forEach { date ->
                byDate.getOrPut(date) { mutableListOf() }.add(item)
            }
        }

        val strip = (0 until STRIP_DAYS).map { offset ->
            val date = today.plusDays(offset.toLong())
            val domainsThatDay = buildList {
                addAll(byDate[date].orEmpty().map { it.domain })
                if (daily.isNotEmpty()) addAll(daily.map { it.domain })
            }.distinctBy { it.id }.sortedBy { it.sortOrder }
            StripDay(date = date, isToday = date == today, domains = domainsThatDay)
        }

        val days = byDate
            .filterKeys { !it.isBefore(today) }
            .map { (date, list) ->
                PlanDay(date, list.sortedBy { it.task.name.lowercase() })
            }

        return PlanUiState(
            today = today,
            strip = strip,
            days = days,
            dailyCount = daily.size,
            loaded = true
        )
    }

    companion object {
        private const val DAYS_AHEAD = 13L // hoy + 13 = 14 jornadas
        private const val STRIP_DAYS = 7

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PlanViewModel(appContainer().taskRepository) }
        }
    }
}
