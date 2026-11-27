package com.example.crissim.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.crissim.data.local.converters.Converters
import java.io.Serializable

/**
 * Micro-entrenamiento diario (HU-24).
 * Tarjeta de protocolo con pregunta, opciones y feedback.
 */
@Entity(tableName = "micro_training")
@TypeConverters(Converters::class)
data class MicroTraining(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val protocolId: String,
    val title: String,
    val question: String,
    val options: List<MicroOption>,
    val correctOptionIndex: Int,
    val explanation: String,
    val protocolSource: String,
    val difficulty: DifficultyLevel,
    val tags: List<String> = emptyList(),
    val order: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

/**
 * Opción de respuesta para micro-entrenamiento
 */
data class MicroOption(
    val text: String,
    val isCorrect: Boolean = false,
    val feedback: String = ""
) : Serializable

/**
 * Insignia/Badge ganada por logros (HU-24)
 */
@Entity(tableName = "badge")
data class Badge(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val iconRes: String,
    val category: BadgeCategory,
    val requirement: String,
    val iconResId: Int = 0,
    val earnedAt: Long = 0,
    val isSecret: Boolean = false,
    val order: Int = 0
) : Serializable

/**
 * Racha de práctica del usuario (HU-24)
 */
@Entity(tableName = "streak")
data class Streak(
    @PrimaryKey
    val userId: Long,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastPracticeDate: Long = 0,
    val totalPracticeDays: Int = 0,
    val lastMicroTrainingId: Long = 0,
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable

/**
 * Progreso del usuario en un micro-entrenamiento específico
 */
@Entity(tableName = "user_micro_training")
data class UserMicroTraining(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val microTrainingId: Long,
    val status: MicroTrainingStatus,
    val attempts: Int = 0,
    val bestScore: Int = 0,
    val lastAttemptAt: Long = 0,
    val completedAt: Long = 0,
    val masteredAt: Long = 0,
    @TypeConverters(Converters::class)
    @ColumnInfo(name = "sm2DataJson")
    val sm2Data: Sm2Data = Sm2Data()
) : Serializable

/**
 * Datos del algoritmo SM-2 (SuperMemo 2) para repetición espaciada
 */
data class Sm2Data(
    val easeFactor: Float = 2.5f,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReviewDate: Long = 0,
    val lastQuality: Int = 0
) : Serializable

/**
 * Relación usuario-insignia para tracking de insignias ganadas
 */
@Entity(
    tableName = "user_badge",
    primaryKeys = ["userId", "badgeId"],
    foreignKeys = [
        ForeignKey(
            entity = Streak::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Badge::class,
            parentColumns = ["id"],
            childColumns = ["badgeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UserBadge(
    val userId: Long,
    val badgeId: String,
    val earnedAt: Long = System.currentTimeMillis()
) : Serializable
