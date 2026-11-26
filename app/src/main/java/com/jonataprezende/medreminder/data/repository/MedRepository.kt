package com.jonataprezende.medreminder.data.repository

import android.content.Context
import com.jonataprezende.medreminder.data.dao.DoseLogDao
import com.jonataprezende.medreminder.data.dao.MedicationDao
import com.jonataprezende.medreminder.data.dao.ScheduleDao
import com.jonataprezende.medreminder.data.database.AppDatabase
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.entity.ScheduleType
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import java.util.Calendar

private const val DAY_MS = 24L * 60 * 60 * 1000

class MedRepository private constructor(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseLogDao: DoseLogDao
) {
    companion object {
        @Volatile
        private var INSTANCE: MedRepository? = null

        fun getInstance(context: Context): MedRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getInstance(context.applicationContext)
                val instance = MedRepository(
                    medicationDao = database.medicationDao(),
                    scheduleDao = database.scheduleDao(),
                    doseLogDao = database.doseLogDao()
                )
                INSTANCE = instance
                instance
            }
        }
    }

    // ==================== MEDICATION ====================

    suspend fun insertMedication(medication: Medication): Long = medicationDao.insert(medication)

    suspend fun updateMedication(medication: Medication) = medicationDao.update(medication)

    suspend fun deleteMedication(medicationId: Long) = medicationDao.deleteById(medicationId)

    suspend fun getMedicationById(medicationId: Long): Medication? = medicationDao.getById(medicationId)

    fun getAllActiveMedications(): Flow<List<Medication>> = medicationDao.getAllActive()

    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAll()

    suspend fun getActiveMedicationCount(): Int = medicationDao.countActive()

    // ==================== SCHEDULE ====================

    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insert(schedule)

    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.update(schedule)

    suspend fun deleteSchedule(scheduleId: Long) {
        scheduleDao.deleteById(scheduleId)
        doseLogDao.deleteByScheduleId(scheduleId)
    }

    suspend fun deleteSchedulesByMedication(medicationId: Long) =
        scheduleDao.deleteByMedicationId(medicationId)

    suspend fun getScheduleById(scheduleId: Long): Schedule? = scheduleDao.getById(scheduleId)

    suspend fun getActiveSchedulesByMedication(medicationId: Long): List<Schedule> =
        scheduleDao.getActiveByMedicationId(medicationId)

    fun getAllActiveSchedules(): Flow<List<Schedule>> = scheduleDao.getAllActive()

    // ==================== DOSE LOG ====================

    suspend fun insertDoseLog(doseLog: DoseLog): Long = doseLogDao.insert(doseLog)

    suspend fun insertDoseLogs(doseLogs: List<DoseLog>): List<Long> = doseLogDao.insertAll(doseLogs)

    suspend fun updateDoseLog(doseLog: DoseLog) = doseLogDao.update(doseLog)

    suspend fun updateDoseLogs(doseLogs: List<DoseLog>) = doseLogDao.updateAll(doseLogs)

    suspend fun getDoseLogById(doseLogId: Long): DoseLog? = doseLogDao.getById(doseLogId)

    suspend fun getDoseLogsBySchedule(scheduleId: Long): List<DoseLog> =
        doseLogDao.getByScheduleId(scheduleId)

    suspend fun getPendingDoseLogsBySchedule(scheduleId: Long): List<DoseLog> =
        doseLogDao.getByScheduleIdAndStatus(scheduleId, DoseStatus.PENDING)

    suspend fun getOverduePendingLogs(): List<DoseLog> =
        doseLogDao.getOverduePending(System.currentTimeMillis(), DoseStatus.PENDING)

    suspend fun getSnoozedReadyLogs(): List<DoseLog> =
        doseLogDao.getSnoozedReady(System.currentTimeMillis(), DoseStatus.PENDING)

    fun getDoseLogsByDateRange(startTime: Long, endTime: Long): Flow<List<DoseLog>> =
        doseLogDao.getByDateRange(startTime, endTime)

    suspend fun getDoseLogsByMedicationAndDateRange(
        medicationId: Long,
        startTime: Long,
        endTime: Long
    ): List<DoseLog> = doseLogDao.getByMedicationAndDateRange(medicationId, startTime, endTime)

    suspend fun getRecentLogs(startTime: Long, endTime: Long, limit: Int = 100): List<DoseLog> =
        doseLogDao.getRecentLogs(startTime, endTime, limit)

    // ==================== DOSE LOG GENERATION ====================

    /**
     * Gera DoseLogs para um schedule a partir de agora até o fim do tratamento
     * (ou 30 dias para uso contínuo). Deve ser chamado após criar/atualizar um schedule.
     */
    suspend fun generateDoseLogsForSchedule(schedule: Schedule, medication: Medication): List<DoseLog> {
        val scheduleId = schedule.id
        if (scheduleId == 0L) return emptyList()

        val now = System.currentTimeMillis()
        val startOfToday = DateTimeUtils.startOfDay(now)
        val endTime = if (schedule.endDate > 0) schedule.endDate else startOfToday + 30 * DAY_MS

        val triggerTimes = calculateTriggerTimes(schedule, startOfToday, endTime)
        val logs = triggerTimes
            .filter { it >= now }
            .map { DoseLog(scheduleId = scheduleId, scheduledAt = it, status = DoseStatus.PENDING) }

        if (logs.isNotEmpty()) {
            doseLogDao.insertAll(logs)
        }
        return logs
    }

    /** Remove todos os DoseLogs futuros pendentes de um schedule (antes de regenerar). */
    suspend fun clearFutureDoseLogs(scheduleId: Long) {
        val now = System.currentTimeMillis()
        doseLogDao.getByScheduleId(scheduleId)
            .filter { it.scheduledAt >= now && it.status == DoseStatus.PENDING }
            .forEach { log -> doseLogDao.deleteById(log.id) }
    }

    /** Regenera logs para todos os schedules ativos (usado no boot). */
    suspend fun regenerateAllDoseLogs() {
        val schedules = scheduleDao.getAllActive().first()
        for (schedule in schedules) {
            val medication = medicationDao.getById(schedule.medicationId)
            if (medication != null && medication.isActive) {
                clearFutureDoseLogs(schedule.id)
                generateDoseLogsForSchedule(schedule, medication)
            }
        }
    }

    // ==================== HELPER METHODS ====================

    private fun calculateTriggerTimes(
        schedule: Schedule,
        startInclusive: Long,
        endInclusive: Long
    ): List<Long> {
        val times = mutableListOf<Long>()
        val calendar = DateTimeUtils.getCalendar()

        when (schedule.type) {
            ScheduleType.FIXED_TIME -> {
                val timeStrings = parseTimes(schedule.timesJson)
                var currentDay = startInclusive
                while (currentDay <= endInclusive) {
                    for (timeStr in timeStrings) {
                        val parsed = parseTime(timeStr) ?: continue
                        calendar.timeInMillis = currentDay
                        calendar.set(Calendar.HOUR_OF_DAY, parsed.first)
                        calendar.set(Calendar.MINUTE, parsed.second)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        val trigger = calendar.timeInMillis
                        if (trigger in startInclusive..endInclusive) times.add(trigger)
                    }
                    currentDay += DAY_MS
                }
            }
            ScheduleType.INTERVAL -> {
                val intervalMs = schedule.intervalHours.toLong() * 60 * 60 * 1000
                if (intervalMs <= 0) return emptyList()
                var nextTrigger = schedule.startDate
                if (nextTrigger < startInclusive) {
                    val diff = startInclusive - nextTrigger
                    val intervalsPassed = (diff + intervalMs - 1) / intervalMs
                    nextTrigger += intervalsPassed * intervalMs
                }
                while (nextTrigger <= endInclusive) {
                    if (nextTrigger >= startInclusive) times.add(nextTrigger)
                    nextTrigger += intervalMs
                }
            }
        }
        return times.sorted()
    }

    private fun parseTimes(timesJson: String): List<String> {
        val result = mutableListOf<String>()
        try {
            val array = JSONArray(timesJson)
            for (i in 0 until array.length()) {
                val value = array.optString(i, "")
                if (value.isNotBlank()) result.add(value)
            }
        } catch (_: Exception) {
            // JSON inválido -> lista vazia
        }
        return result
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour to minute
    }
}
