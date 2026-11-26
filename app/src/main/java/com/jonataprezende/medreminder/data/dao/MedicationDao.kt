package com.jonataprezende.medreminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jonataprezende.medreminder.data.entity.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(medication: Medication): Long

    @Update
    suspend fun update(medication: Medication): Int

    @Query("DELETE FROM medications WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getAllActive(): Flow<List<Medication>>

    @Query("SELECT * FROM medications ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Medication>>

    @Query("SELECT COUNT(*) FROM medications WHERE isActive = 1")
    suspend fun countActive(): Int
}