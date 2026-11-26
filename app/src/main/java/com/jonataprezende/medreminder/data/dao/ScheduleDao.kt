package com.jonataprezende.medreminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jonataprezende.medreminder.data.entity.Schedule
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(schedule: Schedule): Long

    @Update
    suspend fun update(schedule: Schedule): Int

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM schedules WHERE medicationId = :medicationId")
    suspend fun deleteByMedicationId(medicationId: Long): Int

    @Query("SELECT * FROM schedules WHERE id = :id")
    suspend fun getById(id: Long): Schedule?

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId AND isActive = 1")
    suspend fun getActiveByMedicationId(medicationId: Long): List<Schedule>

    @Query("SELECT * FROM schedules WHERE isActive = 1")
    fun getAllActive(): Flow<List<Schedule>>

    @Query("SELECT * FROM schedules WHERE medicationId = :medicationId ORDER BY id")
    suspend fun getAllByMedicationId(medicationId: Long): List<Schedule>
}