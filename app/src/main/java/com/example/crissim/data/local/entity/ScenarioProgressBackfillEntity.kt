package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/** Guarda si el historial previo a la versión 3 ya se proyectó a progreso por escenario. */
@Entity(
    tableName = "scenario_progress_backfill",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScenarioProgressBackfillEntity(
    @PrimaryKey val userId: Long,
    val completedAt: Long = System.currentTimeMillis()
)
