package com.marcm.cadencia.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Consultas de tareas. Las que devuelven [TaskFull] arrastran categoría y ámbito, para
 * que la UI no tenga que resolver el ámbito fila a fila.
 *
 * Sólo se consideran "en juego" las tareas activas de ámbitos activos: desactivar un
 * ámbito lo saca de Hoy y del Plan sin borrar nada.
 */
@Dao
interface TaskDao {

    @Transaction
    @Query(
        """
        SELECT t.* FROM tasks t
        JOIN categories c ON c.id = t.categoryId
        JOIN domains d ON d.id = c.domainId
        WHERE t.isActive = 1 AND d.isActive = 1
        ORDER BY t.dueDateEpochDay, t.name COLLATE NOCASE
        """
    )
    fun observeActiveTasks(): Flow<List<TaskFull>>

    @Transaction
    @Query(
        """
        SELECT t.* FROM tasks t
        JOIN categories c ON c.id = t.categoryId
        JOIN domains d ON d.id = c.domainId
        WHERE t.isActive = 1 AND d.isActive = 1
        ORDER BY t.dueDateEpochDay, t.name COLLATE NOCASE
        """
    )
    suspend fun getActiveTasks(): List<TaskFull>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeTask(id: Long): Flow<TaskFull?>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTask(id: Long): TaskFull?

    /** Tareas vencidas o de hoy con recordatorio, para agrupar las notificaciones. */
    @Transaction
    @Query(
        """
        SELECT t.* FROM tasks t
        JOIN categories c ON c.id = t.categoryId
        JOIN domains d ON d.id = c.domainId
        WHERE t.isActive = 1 AND d.isActive = 1
          AND t.reminderMinute IS NOT NULL
          AND t.dueDateEpochDay <= :epochDay
        ORDER BY t.reminderMinute, d.sortOrder
        """
    )
    suspend fun getDueWithReminder(epochDay: Long): List<TaskFull>

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun count(): Int

    /**
     * Tareas que existen en un ámbito, esté activo o no. Al activar un ámbito se mira
     * esto para no volver a sembrar las sugerencias encima de las que ya hay.
     */
    @Query(
        """
        SELECT COUNT(*) FROM tasks t
        JOIN categories c ON c.id = t.categoryId
        WHERE c.domainId = :domainId
        """
    )
    suspend fun countForDomain(domainId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    /** Deja la tarea apuntando a su siguiente fecha tras completarla (o al deshacerlo). */
    @Query("UPDATE tasks SET dueDateEpochDay = :dueEpochDay, lastCompletedEpochDay = :lastCompletedEpochDay WHERE id = :id")
    suspend fun setSchedule(id: Long, dueEpochDay: Long, lastCompletedEpochDay: Long?)
}
