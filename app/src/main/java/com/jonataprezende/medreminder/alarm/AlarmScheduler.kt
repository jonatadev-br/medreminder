package com.jonataprezende.medreminder.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.Schedule
import com.jonataprezende.medreminder.data.repository.MedRepository
import com.jonataprezende.medreminder.util.DateTimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class AlarmScheduler private constructor(
    private val context: Context,
    private val repository: MedRepository
) {
    private val TAG = "AlarmScheduler"
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        @Volatile
        private var INSTANCE: AlarmScheduler? = null

        fun getInstance(context: Context): AlarmScheduler {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = AlarmScheduler(appContext, MedRepository.getInstance(appContext))
                INSTANCE = instance
                instance
            }
        }
    }

    /** Agenda um alarme único para um DoseLog específico. */
    fun scheduleSingleDose(doseLog: DoseLog, triggerAtMillis: Long) {
        val medicationId = getMedicationIdForSchedule(doseLog.scheduleId)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_DOSE_LOG_ID, doseLog.id)
            putExtra(NotificationHelper.EXTRA_SCHEDULE_ID, doseLog.scheduleId)
            putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationId)
        }

        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            doseLog.id.toInt(),
            intent,
            flags
        )

        setExactAlarm(triggerAtMillis, pendingIntent)
        Log.d(TAG, "Alarme agendado para doseLog ${doseLog.id} às ${DateTimeUtils.formatTime(triggerAtMillis)}")
    }

    /** Cancela um alarme específico. */
    fun cancelSingleDose(doseLogId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getBroadcast(context, doseLogId.toInt(), intent, flags)
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarme cancelado para doseLog $doseLogId")
    }

    /** Reagenda todos os alarmes para doses pendentes futuras (boot / após alterações). */
    fun rescheduleAll() {
        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()
            val schedules = repository.getAllActiveSchedules().first()
            var totalAlarms = 0

            for (schedule in schedules) {
                val medication = repository.getMedicationById(schedule.medicationId)
                if (medication != null && medication.isActive) {
                    val logs = repository.getPendingDoseLogsBySchedule(schedule.id)
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

    /** Gera os DoseLogs de um schedule e agenda os alarmes das doses futuras. */
    fun rescheduleForSchedule(schedule: Schedule) {
        CoroutineScope(Dispatchers.IO).launch {
            val medication = repository.getMedicationById(schedule.medicationId) ?: return@launch
            val logs = repository.generateDoseLogsForSchedule(schedule, medication)
            val now = System.currentTimeMillis()
            for (log in logs) {
                if (log.scheduledAt >= now) scheduleSingleDose(log, log.scheduledAt)
            }
        }
    }

    /** Cancela todos os alarmes pendentes de um schedule. */
    fun cancelForSchedule(scheduleId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val logs = repository.getPendingDoseLogsBySchedule(scheduleId)
            for (log in logs) cancelSingleDose(log.id)
        }
    }

    /** Agenda alarme para dose adiada (snooze). */
    fun scheduleSnooze(doseLog: DoseLog, snoozeUntil: Long) {
        scheduleSingleDose(doseLog, snoozeUntil)
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                } else {
                    // Sem permissão de alarme exato: usa alarme inexato como fallback.
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Sem permissão para alarme exato, usando alarme padrão", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun getMedicationIdForSchedule(scheduleId: Long): Long {
        return runBlocking(Dispatchers.IO) {
            repository.getScheduleById(scheduleId)?.medicationId ?: -1L
        }
    }
}
