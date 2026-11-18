package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: progreso del usuario en un nivel (RF-04, RF-05).
 */
@Entity(
    tableName = "level_progress",
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
        Index(value = ["userId", "level"], unique = true)
    ]
)
data class LevelProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val level: String,         // "BASIC", "INTERMEDIATE", "ADVANCED"
    val isCompleted: Boolean = false,
    val bestScore: Int = 0,
    val maxScore: Int = 0,
    val attempts: Int = 0,
    val lastAttemptAt: Long? = null
)
