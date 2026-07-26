package com.marcm.cadencia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY sortOrder, name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE domainId = :domainId ORDER BY sortOrder, name COLLATE NOCASE")
    suspend fun getForDomain(domainId: Long): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE domainId = :domainId AND key = :key")
    suspend fun getByKey(domainId: Long, key: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}
