package com.marcm.cadencia.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.marcm.cadencia.domain.model.Completion
import java.time.Instant
import java.time.LocalDate

/**
 * Entidad Room del historial de completados. Si se borra la tarea, se borran sus
 * completados en cascada.
 *
 * Guarda la fecha prevista además de la real: es lo que permite decir "a tiempo" o
 * "con retraso" sin recorrer la cadena entera de repeticiones.
 */
@Entity(
    tableName = "completions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId")]
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long,
    val dueDateEpochDay: Long,
    val completedAtEpochMillis: Long,
    val onTime: Boolean
)

fun CompletionEntity.toDomain(): Completion = Completion(
    id = id,
    taskId = taskId,
    dueDate = LocalDate.ofEpochDay(dueDateEpochDay),
    completedAt = Instant.ofEpochMilli(completedAtEpochMillis),
    onTime = onTime
)
