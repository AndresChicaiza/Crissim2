package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.crissim.domain.model.UserRole

/**
 * Entidad Room: perfil de usuario local (RF-08, A7).
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val role: String = UserRole.STUDENT.name,
    val language: String = "ES",
    val createdAt: Long = System.currentTimeMillis()
)
