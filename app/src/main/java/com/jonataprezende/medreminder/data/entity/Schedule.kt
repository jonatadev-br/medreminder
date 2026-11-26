package com.jonataprezende.medreminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId"), Index("isActive")]
)
@Serializable
data class Schedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val medicationId: Long,
    val type: ScheduleType = ScheduleType.FIXED_TIME,
    val timesJson: String = "[]", // JSON array of "HH:MM" strings for FIXED_TIME
    val intervalHours: Int = 0,   // 6, 8, 12 for INTERVAL
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = 0,        // 0 = continuous
    val isActive: Boolean = true
)

enum class ScheduleType {
    FIXED_TIME,
    INTERVAL
}