package com.marcm.cadencia.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainDao {

    @Query("SELECT * FROM domains ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE isActive = 1 ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeActive(): Flow<List<DomainEntity>>

    @Query("SELECT * FROM domains WHERE isActive = 1 ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getActive(): List<DomainEntity>

    @Query("SELECT * FROM domains ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getAll(): List<DomainEntity>

    @Query("SELECT * FROM domains WHERE key = :key")
    suspend fun getByKey(key: String): DomainEntity?

    @Query("SELECT * FROM domains WHERE id = :id")
    suspend fun getById(id: Long): DomainEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(domain: DomainEntity): Long

    @Update
    suspend fun update(domain: DomainEntity)

    @Query("UPDATE domains SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Delete
    suspend fun delete(domain: DomainEntity)
}
