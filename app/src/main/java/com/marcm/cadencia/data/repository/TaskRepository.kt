package com.marcm.cadencia.data.repository

import com.marcm.cadencia.data.local.CompletionDao
import com.marcm.cadencia.data.local.CompletionEntity
import com.marcm.cadencia.data.local.TaskDao
import com.marcm.cadencia.data.local.toDomain
import com.marcm.cadencia.data.local.toEntity
import com.marcm.cadencia.domain.model.Completion
import com.marcm.cadencia.domain.model.Task
import com.marcm.cadencia.domain.model.TaskWithContext
import com.marcm.cadencia.domain.recurrence.Schedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Punto único de acceso a tareas y completados. Traduce entre entidades Room y modelos
 * de dominio y encapsula el ciclo de "marcar hecho": registrar el historial y mover la
 * fecha prevista según el modo de anclaje de la tarea.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val completionDao: CompletionDao,
    private val zone: ZoneId = ZoneId.systemDefault()
) {

    fun observeActiveTasks(): Flow<List<TaskWithContext>> =
        taskDao.observeActiveTasks().map { list -> list.map { it.toDomain() } }

    fun observeTask(id: Long): Flow<TaskWithContext?> =
        taskDao.observeTask(id).map { it?.toDomain() }

    fun observeCompletions(taskId: Long): Flow<List<Completion>> =
        completionDao.observeForTask(taskId).map { list -> list.map { it.toDomain() } }

    suspend fun getTask(id: Long): TaskWithContext? = taskDao.getTask(id)?.toDomain()

    suspend fun getActiveTasks(): List<TaskWithContext> =
        taskDao.getActiveTasks().map { it.toDomain() }

    suspend fun getDueWithReminder(today: LocalDate): List<TaskWithContext> =
        taskDao.getDueWithReminder(today.toEpochDay()).map { it.toDomain() }

    suspend fun completionsFor(taskId: Long): List<Completion> =
        completionDao.getForTask(taskId).map { it.toDomain() }

    suspend fun count(): Int = taskDao.count()

    suspend fun upsert(task: Task): Long =
        if (task.id == 0L) taskDao.insert(task.toEntity())
        else { taskDao.update(task.toEntity()); task.id }

    suspend fun delete(task: Task) = taskDao.delete(task.toEntity())

    /**
     * Marca la tarea como hecha: guarda en el historial la fecha prevista y la real
     * (con su "a tiempo") y adelanta la fecha prevista al siguiente ciclo.
     *
     * @return la tarea ya actualizada, para que quien llame no tenga que releerla.
     */
    suspend fun complete(task: Task, now: Instant = Instant.now()): Task {
        val today = now.atZone(zone).toLocalDate()
        completionDao.insert(
            CompletionEntity(
                taskId = task.id,
                dueDateEpochDay = task.dueDate.toEpochDay(),
                completedAtEpochMillis = now.toEpochMilli(),
                onTime = !today.isAfter(task.dueDate)
            )
        )
        val nextDue = Schedule.nextDueAfterCompletion(task, today)
        taskDao.setSchedule(task.id, nextDue.toEpochDay(), today.toEpochDay())
        return task.copy(dueDate = nextDue, lastCompletedAt = today)
    }

    /**
     * Deshace el "hecho hoy": borra el completado de hoy y devuelve la tarea a la fecha
     * que tenía prevista en ese momento, que es justo la que guardó el completado.
     */
    suspend fun undoComplete(task: Task, today: LocalDate = LocalDate.now(zone)): Task {
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val todays = completionDao.getInRange(task.id, start, end) ?: return task
        completionDao.deleteInRange(task.id, start, end)

        val restoredDue = LocalDate.ofEpochDay(todays.dueDateEpochDay)
        val previous = completionDao.getForTask(task.id)
            .firstOrNull()
            ?.let { Instant.ofEpochMilli(it.completedAtEpochMillis).atZone(zone).toLocalDate() }

        taskDao.setSchedule(task.id, restoredDue.toEpochDay(), previous?.toEpochDay())
        return task.copy(dueDate = restoredDue, lastCompletedAt = previous)
    }
}
