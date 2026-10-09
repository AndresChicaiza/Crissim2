package com.example.crissim.data.repository

import com.example.crissim.domain.model.UserProfile

/**
 * Repositorio del perfil de usuario local (RF-08).
 */
interface UserRepository {
    suspend fun createUser(name: String): UserProfile
    suspend fun getActiveUser(): UserProfile?
    suspend fun updateUserName(userId: Long, name: String)
    suspend fun hasUser(): Boolean
}
