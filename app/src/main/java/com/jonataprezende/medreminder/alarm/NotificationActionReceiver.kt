package com.jonataprezende.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

        Log.d(TAG, "Ação recebida: $action - doseLogId: $doseLogId")

        if (doseLogId == -1L) {
            Log.w(TAG, "Extras inválidos na ação de notificação")
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = MedRepository.getInstance(appContext)
                val doseLog = repository.getDoseLogById(doseLogId)

                if (doseLog == null || doseLog.status != DoseStatus.PENDING) {
                    Log.d(TAG, "DoseLog $doseLogId não está mais pendente")
                    return@launch
                }

                when (action) {
                    NotificationHelper.ACTION_TAKE -> handleTakeAction(appContext, repository, doseLog)
                    NotificationHelper.ACTION_SNOOZE -> handleSnoozeAction(appContext, repository, doseLog)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar ação de notificação", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTakeAction(
        context: Context,
        repository: MedRepository,
        doseLog: DoseLog
    ) {
        val updated = doseLog.copy(
            takenAt = System.currentTimeMillis(),
            status = DoseStatus.TAKEN,
            snoozedUntil = null
        )
        repository.updateDoseLog(updated)
        AlarmScheduler.getInstance(context).cancelSingleDose(doseLog.id)
        NotificationHelper.cancelNotification(context, doseLog.id)
        Log.d(TAG, "Marcado como tomado: doseLog ${doseLog.id}")
    }

    private suspend fun handleSnoozeAction(
        context: Context,
        repository: MedRepository,
        doseLog: DoseLog
    ) {
        val snoozeUntil = System.currentTimeMillis() + 10 * 60 * 1000
        val updated = doseLog.copy(snoozedUntil = snoozeUntil, status = DoseStatus.PENDING)
        repository.updateDoseLog(updated)
        AlarmScheduler.getInstance(context).scheduleSnooze(updated, snoozeUntil)
        NotificationHelper.cancelNotification(context, doseLog.id)
        Log.d(TAG, "Adiado por 10 min: doseLog ${doseLog.id}")
    }
}
