package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import androidx.room.Index
import com.example.crissim.domain.model.UserRole

/**
 * Entidad Room: perfil de usuario local (RF-08, A7).
 */
@Entity(
    tableName = "users",
    indices = [
        Index(value = ["firebaseUid"], unique = true),
        Index(value = ["institutionalCode"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val role: String = UserRole.STUDENT.name,
    val language: String = "ES",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'GUEST'")
    val accountType: String = "GUEST",
    val firebaseUid: String? = null,
    /** Código institucional normalizado (mayúsculas). Null en perfiles invitados. */
    val institutionalCode: String? = null,
    /** `pbkdf2_sha256$iteraciones$hash`. Nunca el PIN en claro. */
    val pinHash: String? = null,
    val pinSalt: String? = null,
    @ColumnInfo(defaultValue = "0")
    val failedAttempts: Int = 0,
    /** Epoch ms hasta el que el acceso está bloqueado; 0 = sin bloqueo. */
    @ColumnInfo(defaultValue = "0")
    val lockedUntil: Long = 0
)