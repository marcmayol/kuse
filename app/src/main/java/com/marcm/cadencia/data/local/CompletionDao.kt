package com.marcm.cadencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Insert
    suspend fun insert(completion: CompletionEntity): Long

    @Query("SELECT * FROM completions WHERE taskId = :taskId ORDER BY completedAtEpochMillis DESC")
    fun observeForTask(taskId: Long): Flow<List<CompletionEntity>>

    @Query("SELECT * FROM completions WHERE taskId = :taskId ORDER BY completedAtEpochMillis DESC")
    suspend fun getForTask(taskId: Long): List<CompletionEntity>

    @Query("SELECT * FROM completions ORDER BY completedAtEpochMillis DESC")
    fun observeAll(): Flow<List<CompletionEntity>>

    /** El completado más reciente de la tarea dentro del rango, para poder deshacerlo. */
    @Query(
        """
        SELECT * FROM completions
        WHERE taskId = :taskId AND completedAtEpochMillis >= :startMillis AND completedAtEpochMillis < :endMillis
        ORDER BY completedAtEpochMillis DESC LIMIT 1
        """
    )
    suspend fun getInRange(taskId: Long, startMillis: Long, endMillis: Long): CompletionEntity?

    /** Borra los completados de la tarea en el rango [startMillis, endMillis) (deshacer "hecho hoy"). */
    @Query("DELETE FROM completions WHERE taskId = :taskId AND completedAtEpochMillis >= :startMillis AND completedAtEpochMillis < :endMillis")
    suspend fun deleteInRange(taskId: Long, startMillis: Long, endMillis: Long)
}
