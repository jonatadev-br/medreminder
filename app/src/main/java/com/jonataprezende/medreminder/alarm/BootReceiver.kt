package com.jonataprezende.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jonataprezende.medreminder.MedReminderApplication
import com.jonataprezende.medreminder.data.repository.MedRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "BootReceiver acionado: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Inicializa o repository e reagenda alarmes
                CoroutineScope(Dispatchers.IO).launch {
                    val repository = MedRepository.getInstance(context.applicationContext as MedReminderApplication)
                    
                    // 1. Regenera DoseLogs para todos os schedules ativos
                    repository.regenerateAllDoseLogs()
                    
                    // 2. Agenda alarmes para todas as doses pendentes futuras
                    val alarmScheduler = AlarmScheduler.getInstance(context.applicationContext as MedReminderApplication)
                    alarmScheduler.rescheduleAll()
                    
                    Log.d(TAG, "Reagendamento completo após boot")
                }
            }
        }
    }
}