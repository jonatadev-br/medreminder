package com.jonataprezende.medreminder.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "medications")
@Serializable
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String,
    val notes: String = "",
    val colorHex: String = "#0E9F8A",
    val iconRes: String = "pill",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val ICON_OPTIONS = listOf("pill", "capsule", "drops", "syringe", "inhaler", "patch")
    }
}