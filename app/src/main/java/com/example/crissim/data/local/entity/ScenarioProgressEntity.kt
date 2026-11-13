package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Mejor resultado local por escenario; el nivel viene del JSON del escenario. */
@Entity(
    tableName = "scenario_progress",
    primaryKeys = ["userId", "scenarioId"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("scenarioId"), Index(value = ["userId", "level"])]
)
data class ScenarioProgressEntity(
    val userId: Long,
    val scenarioId: String,
    val level: String,
    val isRequiredForLevel: Boolean = true,
    val bestScore: Int = 0,
    val maxScore: Int = 0,
    val bestPercentage: Int = 0,
    val isCompleted: Boolean = false,
    val bestAttemptUuid: String? = null,
    val bestAttemptAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val scenarioVersion: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
