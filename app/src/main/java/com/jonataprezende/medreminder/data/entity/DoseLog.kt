package com.jonataprezende.medreminder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(
    tableName = "dose_logs",
    foreignKeys = [
        ForeignKey(
            entity = Schedule::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scheduleId"), Index("scheduledAt"), Index("status")]
)
@Serializable
data class DoseLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val scheduledAt: Long,
    val takenAt: Long? = null,
    val status: DoseStatus = DoseStatus.PENDING,
    val snoozedUntil: Long? = null
) {
    val isTaken: Boolean get() = status == DoseStatus.TAKEN
    val isOverdue: Boolean get() = status == DoseStatus.OVERDUE
    val isPending: Boolean get() = status == DoseStatus.PENDING
    val isSkipped: Boolean get() = status == DoseStatus.SKIPPED
}

enum class DoseStatus {
    PENDING,
    TAKEN,
    OVERDUE,
    SKIPPED
}