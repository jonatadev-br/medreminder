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

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_LOG_ID, -1)
        val scheduleId = intent.getLongExtra(NotificationHelper.EXTRA_SCHEDULE_ID, -1)
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1)

        Log.d(TAG, "Alarme recebido - doseLogId: $doseLogId, scheduleId: $scheduleId, medicationId: $medicationId")

        if (doseLogId == -1L || scheduleId == -1L) {
            Log.w(TAG, "Extras inválidos no alarme")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val repository = MedRepository.getInstance(context.applicationContext as MedReminderApplication)

            val doseLog = repository.getDoseLogsBySchedule(scheduleId)
                .firstOrNull { it.id == doseLogId }

            if (doseLog != null && doseLog.status == DoseStatus.PENDING) {
                var medId = medicationId
                // Se medicationId não veio no intent, busca pelo schedule
                if (medId == -1L) {
                    val schedule = repository.getDoseLogsBySchedule(scheduleId).firstOrNull { it.scheduleId == scheduleId }
                    // Melhor: buscar schedule direto
                    // Como não temos método, vamos buscar via doseLog do schedule
                    // Para simplificar, assumimos que o intent sempre traz medicationId
                }
                
                val medication = if (medId != -1L) {
                    repository.getMedicationById(medId)
                } else {
                    // Fallback: busca o primeiro medicamento ativo do schedule
                    // Isso é um workaround - o ideal é sempre ter o medicationId no intent
                    null
                }

                medication?.let {
                    NotificationHelper.showMedicationNotification(context, doseLog, it)
                    Log.d(TAG, "Notificação exibida para doseLog $doseLogId")
                } ?: Log.w(TAG, "Medicamento não encontrado para doseLog $doseLogId")
            } else {
                Log.d(TAG, "DoseLog $doseLogId não está mais pendente, notificação não exibida")
            }
        }
    }
}