package com.jonataprezende.medreminder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DoseLogDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(doseLog: DoseLog): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(doseLogs: List<DoseLog>): List<Long>

    @Update
    suspend fun update(doseLog: DoseLog): Int

    @Update
    suspend fun updateAll(doseLogs: List<DoseLog>): Int

    @Query("DELETE FROM dose_logs WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM dose_logs WHERE scheduleId = :scheduleId")
    suspend fun deleteByScheduleId(scheduleId: Long): Int

    @Query("SELECT * FROM dose_logs WHERE id = :id")
    suspend fun getById(id: Long): DoseLog?

    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId ORDER BY scheduledAt ASC")
    suspend fun getByScheduleId(scheduleId: Long): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE scheduleId = :scheduleId AND status = :status ORDER BY scheduledAt ASC")
    suspend fun getByScheduleIdAndStatus(scheduleId: Long, status: DoseStatus): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE scheduledAt >= :startTime AND scheduledAt <= :endTime ORDER BY scheduledAt ASC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<DoseLog>>

    @Query("SELECT * FROM dose_logs WHERE scheduledAt >= :startTime AND scheduledAt <= :endTime AND scheduleId IN (SELECT id FROM schedules WHERE medicationId = :medicationId) ORDER BY scheduledAt ASC")
    suspend fun getByMedicationAndDateRange(medicationId: Long, startTime: Long, endTime: Long): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE scheduledAt < :now AND status = :pendingStatus")
    suspend fun getOverduePending(now: Long, pendingStatus: DoseStatus): List<DoseLog>

    @Query("SELECT * FROM dose_logs WHERE snoozedUntil IS NOT NULL AND snoozedUntil <= :now AND status = :pendingStatus")
    suspend fun getSnoozedReady(now: Long, pendingStatus: DoseStatus): List<DoseLog>

    // Para exportação/histórico
    @Query("SELECT * FROM dose_logs WHERE scheduledAt >= :startTime AND scheduledAt <= :endTime ORDER BY scheduledAt DESC LIMIT :limit")
    suspend fun getRecentLogs(startTime: Long, endTime: Long, limit: Int): List<DoseLog>

    @Query("SELECT COUNT(*) FROM dose_logs WHERE scheduleId = :scheduleId AND status = :status")
    suspend fun countByScheduleAndStatus(scheduleId: Long, status: DoseStatus): Int
}