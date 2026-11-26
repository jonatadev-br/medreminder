package com.jonataprezende.medreminder.util

import java.util.Calendar
import java.util.TimeZone

object DateTimeUtils {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun getCalendar(): Calendar {
        return Calendar.getInstance(TimeZone.getDefault())
    }

    fun startOfDay(timestamp: Long): Long {
        val cal = getCalendar()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDay(timestamp: Long): Long {
        val cal = getCalendar()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun formatTime(timestamp: Long): String {
        val cal = getCalendar()
        cal.timeInMillis = timestamp
        return "${cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')}:${cal.get(Calendar.MINUTE).toString().padStart(2, '0')}"
    }

    fun formatDate(timestamp: Long): String {
        val cal = getCalendar()
        cal.timeInMillis = timestamp
        return "${cal.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')}/${(cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')}/${cal.get(Calendar.YEAR)}"
    }

    fun formatDateTime(timestamp: Long): String {
        return "${formatDate(timestamp)} ${formatTime(timestamp)}"
    }

    fun formatRelativeDate(timestamp: Long): String {
        val todayStart = startOfDay(System.currentTimeMillis())
        val targetStart = startOfDay(timestamp)
        val diffDays = (targetStart - todayStart) / DAY_MS

        return when (diffDays) {
            0 -> "Hoje"
            1 -> "Amanhã"
            -1 -> "Ontem"
            in -6..-2 -> "${-diffDays} dias atrás"
            in 2..6 -> "Em $diffDays dias"
            else -> formatDate(timestamp)
        }
    }

    fun parseTimeString(timeStr: String): Pair<Int, Int>? {
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1].toInt()
                if (hour in 0..23 && minute in 0..59) {
                    hour to minute
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun isToday(timestamp: Long): Boolean {
        val todayStart = startOfDay(System.currentTimeMillis())
        val targetStart = startOfDay(timestamp)
        return todayStart == targetStart
    }

    fun isOverdue(timestamp: Long): Boolean {
        return timestamp < System.currentTimeMillis()
    }

    fun getNextTriggerTime(scheduleType: String, timesJson: String, intervalHours: Int, startDate: Long, lastTaken: Long?): Long {
        val now = System.currentTimeMillis()
        val cal = getCalendar()

        return when (scheduleType) {
            "FIXED_TIME" -> {
                val timeStrings = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString<List<String>>(timesJson)
                var nextTime = Long.MAX_VALUE
                for (timeStr in timeStrings) {
                    parseTimeString(timeStr)?.let { (hour, minute) ->
                        cal.timeInMillis = now
                        cal.set(Calendar.HOUR_OF_DAY, hour)
                        cal.set(Calendar.MINUTE, minute)
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        var trigger = cal.timeInMillis
                        if (trigger <= now) {
                            trigger += DAY_MS
                        }
                        if (trigger < nextTime) {
                            nextTime = trigger
                        }
                    }
                }
                nextTime
            }
            "INTERVAL" -> {
                val intervalMs = intervalHours.toLong() * 60 * 60 * 1000
                var baseTime = lastTaken ?: startDate
                var nextTrigger = baseTime + intervalMs
                while (nextTrigger <= now) {
                    nextTrigger += intervalMs
                }
                nextTrigger
            }
            else -> now + 60 * 60 * 1000 // fallback 1 hora
        }
    }
}