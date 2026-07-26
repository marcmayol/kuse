package com.marcm.cadencia.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.marcm.cadencia.domain.model.AnchorMode
import com.marcm.cadencia.domain.model.Recurrence
import com.marcm.cadencia.domain.model.RecurrenceType
import com.marcm.cadencia.domain.model.Task
import com.marcm.cadencia.domain.model.TaskWithContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * Entidad Room de una tarea. Los tipos del dominio (enums, fechas, set de días) se
 * guardan como primitivos: enums por nombre, fechas como epoch-day, hora como minuto
 * del día, y los días de la semana como CSV de sus valores 1..7.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("dueDateEpochDay")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val categoryId: Long,
    val iconKey: String?,
    val recurrenceType: String,
    val intervalValue: Int,
    val weekdaysCsv: String,
    val dayOfMonth: Int?,
    val anchorMode: String,
    val reminderMinute: Int?,
    val createdAtEpochDay: Long,
    val dueDateEpochDay: Long,
    val lastCompletedEpochDay: Long?,
    val isActive: Boolean
)

/**
 * Tarea con su categoría y el ámbito de esa categoría. Room resuelve la relación
 * anidada; las consultas que lo devuelven van anotadas con @Transaction.
 */
data class TaskFull(
    @Embedded val task: TaskEntity,
    @Relation(
        entity = CategoryEntity::class,
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryWithDomain
)

data class CategoryWithDomain(
    @Embedded val category: CategoryEntity,
    @Relation(parentColumn = "domainId", entityColumn = "id")
    val domain: DomainEntity
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    name = name,
    categoryId = categoryId,
    iconKey = iconKey,
    recurrence = Recurrence(
        type = RecurrenceType.fromName(recurrenceType),
        interval = intervalValue,
        weekdays = parseWeekdays(weekdaysCsv),
        dayOfMonth = dayOfMonth
    ),
    anchorMode = AnchorMode.fromName(anchorMode),
    reminderTime = reminderMinute?.let { LocalTime.of(it / 60, it % 60) },
    createdAt = LocalDate.ofEpochDay(createdAtEpochDay),
    dueDate = LocalDate.ofEpochDay(dueDateEpochDay),
    lastCompletedAt = lastCompletedEpochDay?.let { LocalDate.ofEpochDay(it) },
    isActive = isActive
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    name = name,
    categoryId = categoryId,
    iconKey = iconKey,
    recurrenceType = recurrence.type.name,
    intervalValue = recurrence.interval,
    weekdaysCsv = recurrence.weekdays.sortedBy { it.value }.joinToString(",") { it.value.toString() },
    dayOfMonth = recurrence.dayOfMonth,
    anchorMode = anchorMode.name,
    reminderMinute = reminderTime?.let { it.hour * 60 + it.minute },
    createdAtEpochDay = createdAt.toEpochDay(),
    dueDateEpochDay = dueDate.toEpochDay(),
    lastCompletedEpochDay = lastCompletedAt?.toEpochDay(),
    isActive = isActive
)

fun TaskFull.toDomain(): TaskWithContext = TaskWithContext(
    task = task.toDomain(),
    category = category.category.toDomain(),
    domain = category.domain.toDomain()
)

private fun parseWeekdays(csv: String): Set<DayOfWeek> =
    csv.split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .map { DayOfWeek.of(it) }
        .toSet()
