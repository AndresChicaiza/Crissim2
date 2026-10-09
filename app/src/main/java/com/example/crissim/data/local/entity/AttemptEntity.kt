package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: resultado de un intento de escenario.
 */
@Entity(
    tableName = "attempts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId"),
        Index("scenarioId")
    ]
)
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scenarioId: String,
    val userId: Long,
    val score: Int,
    val maxScore: Int,
    val isPassed: Boolean,
    val startedAt: Long,
    val completedAt: Long,
    val endNodeId: String,
    val endType: String,
    val decisionsJson: String  // JSON serializado de las decisiones
)
