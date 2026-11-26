package com.jonataprezende.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jonataprezende.medreminder.MedReminderApplication
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.repository.MedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    private val TAG = "NotifActionReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val doseLogId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_LOG_ID, -1)
        val scheduleId = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1)
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1)

        Log.d(TAG, "Ação recebida: $action - doseLogId: $doseLogId")

        if (doseLogId == -1L || scheduleId == -1L) {
            Log.w(TAG, "Extras inválidos na ação de notificação")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val repository = MedRepository.getInstance(context.applicationContext as MedReminderApplication)

            val doseLog = repository.getDoseLogsBySchedule(scheduleId)
                .firstOrNull { it.id == doseLogId }

            if (doseLog == null || doseLog.status != DoseStatus.PENDING) {
                Log.d(TAG, "DoseLog $doseLogId não está mais pendente")
                return
            }

            when (action) {
                NotificationHelper.ACTION_TAKE -> {
                    handleTakeAction(repository, doseLog, scheduleId)
                }
                NotificationHelper.ACTION_SNOOZE -> {
                    handleSnoozeAction(context, repository, doseLog, scheduleId)
                }
            }
        }
    }

    private suspend fun handleTakeAction(
        repository: MedRepository,
        doseLog: DoseLog,
        scheduleId: Long
    ) {
        val now = System.currentTimeMillis()
        val updatedDoseLog = doseLog.copy(
            takenAt = now,
            status = DoseStatus.TAKEN,
            snoozedUntil = null
        )
        repository.updateDoseLog(updatedDoseLog)

        // Cancela qualquer alarme pendente para este doseLog (caso haja snooze anterior)
        val alarmScheduler = AlarmScheduler.getInstance(android.app.Application() as MedReminderApplication)
        alarmScheduler.cancelSingleDose(doseLog.id!!)

        // Para schedules de INTERVALO, o próximo alarme será baseado no lastTaken
        // O AlarmScheduler.rescheduleForSchedule cuidará disso quando o schedule for atualizado
        // Por agora, apenas logamos
        Log.d(TAG, "Marcado como tomado: doseLog ${doseLog.id}")
    }

    private suspend fun handleSnoozeAction(
        context: Context,
        repository: MedRepository,
        doseLog: DoseLog,
        scheduleId: Long
    ) {
        val snoozeUntil = System.currentTimeMillis() + 10 * 60 * 1000 // 10 minutos
        val updatedDoseLog = doseLog.copy(
            snoozedUntil = snoozeUntil,
            status = DoseStatus.PENDING
        )
        repository.updateDoseLog(updatedDoseLog)

        // Reagenda alarme para daqui 10 min
        val alarmScheduler = AlarmScheduler.getInstance(context.applicationContext as MedReminderApplication)
        alarmScheduler.scheduleSnooze(updatedDoseLog, snoozeUntil)

        Log.d(TAG, "Adiado por 10 min: doseLog ${doseLog.id}")
    }
}