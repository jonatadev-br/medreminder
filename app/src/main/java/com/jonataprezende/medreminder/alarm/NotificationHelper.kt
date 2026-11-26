package com.jonataprezende.medreminder.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.jonataprezende.medreminder.R
import com.jonataprezende.medreminder.data.entity.DoseLog
import com.jonataprezende.medreminder.data.entity.DoseStatus
import com.jonataprezende.medreminder.data.entity.Medication
import com.jonataprezende.medreminder.util.DateTimeUtils

object NotificationHelper {
    private const val TAG = "NotificationHelper"
    private const val CHANNEL_ID = "medication_reminders"
    private const val CHANNEL_NAME = "Lembretes de Medicação"
    private const val CHANNEL_DESC = "Notificações de alta prioridade para horários de remédios"

    const val ACTION_TAKE = "com.jonataprezende.medreminder.ACTION_TAKE"
    const val ACTION_SNOOZE = "com.jonataprezende.medreminder.ACTION_SNOOZE"

    const val EXTRA_DOSE_LOG_ID = "extra_dose_log_id"
    const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
    const val EXTRA_MEDICATION_ID = "extra_medication_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setSound(getAlarmSoundUri(context), Notification.AUDIO_ATTRIBUTES_DEFAULT)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getAlarmSoundUri(context: Context): Uri {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    fun showMedicationNotification(
        context: Context,
        doseLog: DoseLog,
        medication: Medication
    ) {
        val takeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKE
            putExtra(EXTRA_DOSE_LOG_ID, doseLog.id)
            putExtra(EXTRA_SCHEDULE_ID, doseLog.scheduleId)
            putExtra(EXTRA_MEDICATION_ID, medication.id)
        }
        val takePendingIntent = PendingIntent.getBroadcast(
            context,
            doseLog.id.toInt(),
            takeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_DOSE_LOG_ID, doseLog.id)
            putExtra(EXTRA_SCHEDULE_ID, doseLog.scheduleId)
            putExtra(EXTRA_MEDICATION_ID, medication.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (doseLog.id + 100000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Full screen intent: mostra em tela cheia na lockscreen
        val fullScreenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = ACTION_TAKE
            putExtra(EXTRA_DOSE_LOG_ID, doseLog.id)
            putExtra(EXTRA_SCHEDULE_ID, doseLog.scheduleId)
            putExtra(EXTRA_MEDICATION_ID, medication.id)
        }
        val fullScreenPendingIntent = PendingIntent.getBroadcast(
            context,
            (doseLog.id + 200000).toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notes = medication.notes.ifBlank { "Toque para confirmar" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_pill)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_content, medication.name, medication.dosage))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${medication.name} - ${medication.dosage}\n$notes\nHorário: ${DateTimeUtils.formatTime(doseLog.scheduledAt)}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setSound(getAlarmSoundUri(context))
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_save,
                    context.getString(R.string.action_take),
                    takePendingIntent
                ).build()
            )
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_media_pause,
                    context.getString(R.string.action_snooze),
                    snoozePendingIntent
                ).build()
            )
            .setColor(context.getColor(R.color.accent))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(doseLog.id.toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Sem permissão POST_NOTIFICATIONS", e)
        }
    }

    fun cancelNotification(context: Context, doseLogId: Long) {
        try {
            NotificationManagerCompat.from(context).cancel(doseLogId.toInt())
        } catch (_: SecurityException) {
        }
    }

    fun cancelAllNotifications(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: SecurityException) {
        }
    }

    fun showTestNotification(context: Context) {
        val medication = Medication(
            name = "Medicamento de Teste",
            dosage = "1 comprimido",
            notes = "Esta é uma notificação de teste"
        )
        val doseLog = DoseLog(
            id = 999999L,
            scheduleId = -1,
            scheduledAt = System.currentTimeMillis(),
            status = DoseStatus.PENDING
        )
        showMedicationNotification(context, doseLog, medication)
    }
}
