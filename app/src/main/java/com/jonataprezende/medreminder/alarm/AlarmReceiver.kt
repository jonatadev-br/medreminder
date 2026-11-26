package com.jonataprezende.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.repository.MedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    private val TAG = "AlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val doseLogId = intent.getLongExtra(NotificationHelper.EXTRA_DOSE_LOG_ID, -1)
        val medicationId = intent.getLongExtra(NotificationHelper.EXTRA_MEDICATION_ID, -1)

        Log.d(TAG, "Alarme recebido - doseLogId: $doseLogId, medicationId: $medicationId")

        if (doseLogId == -1L) {
            Log.w(TAG, "Extras inválidos no alarme")
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = MedRepository.getInstance(appContext)
                val doseLog = repository.getDoseLogById(doseLogId)

                if (doseLog != null && doseLog.status == DoseStatus.PENDING) {
                    val medId = if (medicationId != -1L) {
                        medicationId
                    } else {
                        repository.getScheduleById(doseLog.scheduleId)?.medicationId ?: -1L
                    }
                    val medication = if (medId != -1L) repository.getMedicationById(medId) else null
                    if (medication != null) {
                        NotificationHelper.showMedicationNotification(appContext, doseLog, medication)
                        Log.d(TAG, "Notificação exibida para doseLog $doseLogId")
                    } else {
                        Log.w(TAG, "Medicamento não encontrado para doseLog $doseLogId")
                    }
                } else {
                    Log.d(TAG, "DoseLog $doseLogId não está mais pendente")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar alarme", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
