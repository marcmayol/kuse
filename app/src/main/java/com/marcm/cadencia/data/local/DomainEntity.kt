package com.marcm.cadencia.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.marcm.cadencia.domain.model.Domain

/** Entidad Room de un ámbito. La clave [key] es única y estable. */
@Entity(
    tableName = "domains",
    indices = [Index(value = ["key"], unique = true)]
)
data class DomainEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val key: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val sortOrder: Int,
    val isActive: Boolean,
    val isBuiltIn: Boolean
)

fun DomainEntity.toDomain(): Domain = Domain(
    id = id,
    key = key,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isActive = isActive,
    isBuiltIn = isBuiltIn
)

fun Domain.toEntity(): DomainEntity = DomainEntity(
    id = id,
    key = key,
    name = name,
    iconKey = iconKey,
    colorHex = colorHex,
    sortOrder = sortOrder,
    isActive = isActive,
    isBuiltIn = isBuiltIn
)
