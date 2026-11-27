package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para persistencia del informe de Simulacro en Vivo (HU-25).
 */
@Entity(tableName = "live_drill")
data class LiveDrillEntity(
    @PrimaryKey
    val id: String,
    val userId: Long,
    val userName: String,
    val role: String,
    val startedAt: Long,
    val completedAt: Long,
    val totalDurationSeconds: Long,
    val targetTimeSeconds: Int,
    val incidentsJson: String,
    val isCompleted: Boolean
)
