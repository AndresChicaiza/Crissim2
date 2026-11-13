package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: estado guardado del motor para reanudación (RF-10, A3).
 * Se actualiza en cada transición de nodo.
 */
@Entity(
    tableName = "saved_states",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "scenarioId", "deviceId"], unique = true)
    ]
)
data class SavedStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val scenarioId: String,
    val currentNodeId: String,
    val score: Int,
    val decisionsJson: String,
    val startTime: Long,
    val savedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'legacy-device'")
    val deviceId: String = "legacy-device",
    @ColumnInfo(defaultValue = "0")
    val revision: Long = 0
)
