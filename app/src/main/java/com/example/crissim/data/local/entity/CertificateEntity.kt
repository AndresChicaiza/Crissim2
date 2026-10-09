package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad Room: certificado digital generado (RF-07, A4).
 */
@Entity(
    tableName = "certificates",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("userId")
    ]
)
data class CertificateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val userName: String,
    val level: String,          // "BASIC", "INTERMEDIATE", "ADVANCED"
    val scenarioId: String,
    val score: Int,
    val maxScore: Int,
    val issuedAt: Long = System.currentTimeMillis(),
    val signature: String = "",
    val pdfPath: String? = null
)
