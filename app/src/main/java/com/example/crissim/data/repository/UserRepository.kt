package com.example.crissim.data.repository

import com.example.crissim.domain.model.UserProfile
import com.example.crissim.domain.model.UserRole

/**
 * Repositorio del perfil de usuario local (RF-08).
 */
interface UserRepository {
    suspend fun createUser(name: String, role: UserRole = UserRole.STUDENT, language: String = "ES"): UserProfile
    suspend fun getActiveUser(): UserProfile?
    suspend fun updateUserName(userId: Long, name: String)
    suspend fun updateUserRole(userId: Long, role: UserRole)
    suspend fun updateUserLanguage(userId: Long, language: String)
    suspend fun hasUser(): Boolean
}
