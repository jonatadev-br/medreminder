package com.jonataprezende.medreminder.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
                val appContext = context.applicationContext
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val repository = MedRepository.getInstance(appContext)
                        repository.regenerateAllDoseLogs()
                        AlarmScheduler.getInstance(appContext).rescheduleAll()
                        Log.d(TAG, "Reagendamento completo após boot")
                    } catch (e: Exception) {
                        Log.e(TAG, "Erro no reagendamento pós-boot", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
