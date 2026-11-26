package com.jonataprezende.medreminder.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.jonataprezende.medreminder.MedReminderApplication
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlarmScheduler(
    private val application: Application,
    private val repository: MedRepository
) {
    private val TAG = "AlarmScheduler"
    private val alarmManager: AlarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        @Volatile
        private var INSTANCE: AlarmScheduler? = null

        fun getInstance(application: Application): AlarmScheduler {
            return INSTANCE ?: synchronized(this) {
                val repository = MedRepository.getInstance(application)
                val instance = AlarmScheduler(application, repository)
                INSTANCE = instance
                instance
            }
        }
    }

    /**
     * Agenda um alarme único para um DoseLog específico.
     */
    fun scheduleSingleDose(doseLog: DoseLog, triggerAtMillis: Long) {
        val medicationId = getMedicationIdForSchedule(doseLog.scheduleId)
        
        val intent = Intent(application, AlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_DOSE_LOG_ID, doseLog.id!!)
            putExtra(NotificationHelper.EXTRA_SCHEDULE_ID, doseLog.scheduleId)
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            doseLog.id!!.toInt(),
            intent,
            flags
        )

        setExactAlarm(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Alarme agendado para doseLog ${doseLog.id} em ${DateTimeUtils.formatTime(triggerAtMillis)}")
    }

    /**
     * Cancela um alarme específico.
     */
    fun cancelSingleDose(doseLogId: Long) {
        val intent = Intent(application, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(
            application,
            doseLogId.toInt(),
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarme cancelado para doseLog $doseLogId")
    }

    /**
     * Reagenda todos os alarmes para doses pendentes futuras.
     * Chamado no boot e quando schedules são criados/atualizados.
     */
    fun rescheduleAll() {
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val schedules = repository.getAllActiveSchedules().value ?: emptyList()
            var totalAlarms = 0

            for (schedule in schedules) {
                val medication = repository.getMedicationById(schedule.medicationId)
                if (medication != null && medication.isActive) {
                    val logs = repository.getPendingDoseLogsBySchedule(schedule.id!!)
                        .filter { it.scheduledAt >= now }
                    for (log in logs) {
                        scheduleSingleDose(log, log.scheduledAt)
                        totalAlarms++
                    }
                }
            }
            Log.d(TAG, "Reagendados $totalAlarms alarmes no total")
        }
    }

    /**
     * Reagenda alarmes para um schedule específico (após criar/atualizar schedule).
     */
    fun rescheduleForSchedule(schedule: Schedule) {
        CoroutineScope(Dispatchers.IO).launch {
            val medication = repository.getMedicationById(schedule.medicationId)
            medication?.let {
                val logs = repository.generateDoseLogsForSchedule(schedule, medication)
                val now = System.currentTimeMillis()
                for (log in logs) {
                    if (log.scheduledAt >= now) {
                        scheduleSingleDose(log, log.scheduledAt)
                    }
                }
            }
        }
    }

    /**
     * Cancela todos os alarmes de um schedule (antes de deletar/atualizar).
     */
    fun cancelForSchedule(scheduleId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val logs = repository.getPendingDoseLogsBySchedule(scheduleId)
            for (log in logs) {
                cancelSingleDose(log.id!!)
            }
        }
    }

    /**
     * Agenda alarme para dose adiada (snooze).
     */
    fun scheduleSnooze(doseLog: DoseLog, snoozeUntil: Long) {
        scheduleSingleDose(doseLog, snoozeUntil)
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                Log.w(TAG, "Permissão SCHEDULE_EXACT_ALARM não concedida")
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun getMedicationIdForSchedule(scheduleId: Long): Long {
        // Busca síncrona via runBlocking (OK aqui pois é chamado de corrotina IO)
        return runBlocking(Dispatchers.IO) {
            val schedules = repository.getAllActiveSchedules().value ?: emptyList()
            schedules.firstOrNull { it.id == scheduleId }?.medicationId ?: -1L
        }
    }
}