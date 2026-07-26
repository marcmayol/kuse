package com.marcm.cadencia.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.marcm.cadencia.domain.model.Category

/** Entidad Room de una categoría. Al borrar el ámbito se van sus categorías en cascada. */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = DomainEntity::class,
            parentColumns = ["id"],
            childColumns = ["domainId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("domainId"), Index(value = ["domainId", "key"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val domainId: Long,
    val key: String,
    val name: String,
    val iconKey: String?,
    val sortOrder: Int
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    domainId = domainId,
    key = key,
    name = name,
    iconKey = iconKey,
    sortOrder = sortOrder
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    domainId = domainId,
    key = key,
    name = name,
    iconKey = iconKey,
    sortOrder = sortOrder
)
