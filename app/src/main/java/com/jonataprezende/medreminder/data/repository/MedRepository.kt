package com.jonataprezende.medreminder.data.repository

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.jonataprezende.medreminder.data.dao.DoseLogDao
import com.jonataprezende.medreminder.data.dao.MedicationDao
import com.jonataprezende.medreminder.data.dao.ScheduleDao
import com.jonataprezende.medreminder.data.database.AppDatabase
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

class MedRepository private constructor(
    private val medicationDao: MedicationDao,
    private val scheduleDao: ScheduleDao,
    private val doseLogDao: DoseLogDao,
    private val ioScope: CoroutineScope
) {
    companion object {
        @Volatile
        private var INSTANCE: MedRepository? = null

        fun getInstance(application: Application): MedRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getInstance(application)
                val instance = MedRepository(
                    medicationDao = database.medicationDao(),
                    scheduleDao = database.scheduleDao(),
                    doseLogDao = database.doseLogDao(),
                    ioScope = CoroutineScope(Dispatchers.IO)
                )
                INSTANCE = instance
                instance
            }
        }
    }

    // ==================== MEDICATION ====================

    suspend fun insertMedication(medication: Medication): Long {
        return medicationDao.insert(medication)
    }

    suspend fun updateMedication(medication: Medication) {
        medicationDao.update(medication)
    }

    suspend fun deleteMedication(medicationId: Long) {
        // Cascade delete handled by Room foreign keys
        medicationDao.deleteById(medicationId)
    }

    suspend fun getMedicationById(medicationId: Long): Medication? {
        return medicationDao.getById(medicationId)
    }

    fun getAllActiveMedications(): LiveData<List<Medication>> {
        return medicationDao.getAllActive().asLiveData()
    }

    fun getAllMedications(): LiveData<List<Medication>> {
        return medicationDao.getAll().asLiveData()
    }

    suspend fun getActiveMedicationCount(): Int {
        return medicationDao.countActive()
    }

    // ==================== SCHEDULE ====================

    suspend fun insertSchedule(schedule: Schedule): Long {
        return scheduleDao.insert(schedule)
    }

    suspend fun updateSchedule(schedule: Schedule) {
        scheduleDao.update(schedule)
    }

    suspend fun deleteSchedule(scheduleId: Long) {
        scheduleDao.deleteById(scheduleId)
        doseLogDao.deleteByScheduleId(scheduleId)
    }

    suspend fun deleteSchedulesByMedication(medicationId: Long) {
        scheduleDao.deleteByMedicationId(medicationId)
    }

    suspend fun getActiveSchedulesByMedication(medicationId: Long): List<Schedule> {
        return scheduleDao.getActiveByMedicationId(medicationId)
    }

    fun getAllActiveSchedules(): LiveData<List<Schedule>> {
        return scheduleDao.getAllActive().asLiveData()
    }

    // ==================== DOSE LOG ====================

    suspend fun insertDoseLog(doseLog: DoseLog): Long {
        return doseLogDao.insert(doseLog)
    }

    suspend fun insertDoseLogs(doseLogs: List<DoseLog>): List<Long> {
        return doseLogDao.insertAll(doseLogs)
    }

    suspend fun updateDoseLog(doseLog: DoseLog) {
        doseLogDao.update(doseLog)
    }

    suspend fun updateDoseLogs(doseLogs: List<DoseLog>) {
        doseLogDao.updateAll(doseLogs)
    }

    suspend fun getDoseLogsBySchedule(scheduleId: Long): List<DoseLog> {
        return doseLogDao.getByScheduleId(scheduleId)
    }

    suspend fun getPendingDoseLogsBySchedule(scheduleId: Long): List<DoseLog> {
        return doseLogDao.getByScheduleIdAndStatus(scheduleId, DoseStatus.PENDING)
    }

    suspend fun getOverduePendingLogs(): List<DoseLog> {
        val now = System.currentTimeMillis()
        return doseLogDao.getOverduePending(now, DoseStatus.PENDING)
    }

    suspend fun getSnoozedReadyLogs(): List<DoseLog> {
        val now = System.currentTimeMillis()
        return doseLogDao.getSnoozedReady(now, DoseStatus.PENDING)
    }

    fun getDoseLogsByDateRange(startTime: Long, endTime: Long): LiveData<List<DoseLog>> {
        return doseLogDao.getByDateRange(startTime, endTime).asLiveData()
    }

    suspend fun getDoseLogsByMedicationAndDateRange(
        medicationId: Long,
        startTime: Long,
        endTime: Long
    ): List<DoseLog> {
        return doseLogDao.getByMedicationAndDateRange(medicationId, startTime, endTime)
    }

    suspend fun getRecentLogs(startTime: Long, endTime: Long, limit: Int = 100): List<DoseLog> {
        return doseLogDao.getRecentLogs(startTime, endTime, limit)
    }

    // ==================== DOSE LOG GENERATION ====================

    /**
     * Gera DoseLogs para um schedule a partir de agora até o fim do tratamento (ou 30 dias para contínuo).
     * Deve ser chamado após criar/atualizar um schedule.
     */
    suspend fun generateDoseLogsForSchedule(schedule: Schedule, medication: Medication): List<DoseLog> {
        val now = System.currentTimeMillis()
        val startOfToday = DateTimeUtils.startOfDay(now)
        val endTime = if (schedule.endDate > 0) schedule.endDate else startOfToday + 30L * 24 * 60 * 60 * 1000 // 30 dias

        val logs = mutableListOf<DoseLog>()
        val triggerTimes = calculateTriggerTimes(schedule, startOfToday, endTime)

        for (triggerTime in triggerTimes) {
            if (triggerTime >= now) {
                // Só cria logs para o futuro (incluindo hoje)
                logs.add(DoseLog(
                    scheduleId = schedule.id!!,
                    scheduledAt = triggerTime,
                    status = DoseStatus.PENDING
                ))
            }
        }

        if (logs.isNotEmpty()) {
            doseLogDao.insertAll(logs)
        }
        return logs
    }

    /**
     * Remove todos os DoseLogs futuros de um schedule (usado antes de regenerar).
     */
    suspend fun clearFutureDoseLogs(scheduleId: Long) {
        val now = System.currentTimeMillis()
        val futureLogs = doseLogDao.getByScheduleId(scheduleId)
            .filter { it.scheduledAt >= now && it.status == DoseStatus.PENDING }
        for (log in futureLogs) {
            doseLogDao.deleteById(log.id!!)
        }
    }

    /**
     * Regenera logs para todos os schedules ativos (usado no boot).
     */
    suspend fun regenerateAllDoseLogs() {
        val schedules = scheduleDao.getAllActive().first()
        for (schedule in schedules) {
            val medication = medicationDao.getById(schedule.medicationId)
            if (medication != null && medication.isActive) {
                clearFutureDoseLogs(schedule.id!!)
                generateDoseLogsForSchedule(schedule, medication)
            }
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Calcula todos os horários de disparo para um schedule entre startInclusive e endInclusive.
     */
    private fun calculateTriggerTimes(schedule: Schedule, startInclusive: Long, endInclusive: Long): List<Long> {
        val times = mutableListOf<Long>()
        val calendar = DateTimeUtils.getCalendar()

        when (schedule.type) {
            ScheduleType.FIXED_TIME -> {
                val timeStrings = Json { ignoreUnknownKeys = true }.decodeFromString<List<String>>(schedule.timesJson)
                var currentDay = startInclusive
                while (currentDay <= endInclusive) {
                    for (timeStr in timeStrings) {
                        val (hour, minute) = parseTime(timeStr)
                        calendar.timeInMillis = currentDay
                        calendar.set(calendar.HOUR_OF_DAY, hour)
                        calendar.set(calendar.MINUTE, minute)
                        calendar.set(calendar.SECOND, 0)
                        calendar.set(calendar.MILLISECOND, 0)
                        val triggerTime = calendar.timeInMillis
                        if (triggerTime >= startInclusive && triggerTime <= endInclusive) {
                            times.add(triggerTime)
                        }
                    }
                    currentDay += 24 * 60 * 60 * 1000 // próximo dia
                }
            }
            ScheduleType.INTERVAL -> {
                val intervalMs = schedule.intervalHours.toLong() * 60 * 60 * 1000
                var nextTrigger = schedule.startDate

                // Alinha ao próximo intervalo a partir de startInclusive
                if (nextTrigger < startInclusive) {
                    val diff = startInclusive - nextTrigger
                    val intervalsPassed = (diff + intervalMs - 1) / intervalMs // ceil division
                    nextTrigger += intervalsPassed * intervalMs
                }

                while (nextTrigger <= endInclusive) {
                    if (nextTrigger >= startInclusive) {
                        times.add(nextTrigger)
                    }
                    nextTrigger += intervalMs
                }
            }
        }

        return times.sorted()
    }

    private fun parseTime(timeStr: String): Pair<Int, Int> {
        val parts = timeStr.split(":")
        return parts[0].toInt() to parts[1].toInt()
    }
}