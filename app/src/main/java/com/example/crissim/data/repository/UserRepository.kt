package com.example.crissim.data.repository

import com.example.crissim.domain.model.UserProfile
import com.example.crissim.domain.model.UserRole
import com.example.crissim.domain.security.LocalAuthResult

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

    /** HU-03: registro local sin red. Si el perfil existente es invitado, lo convierte conservando su progreso. */
    suspend fun registerLocal(name: String, institutionalCode: String, pin: String, role: UserRole): LocalAuthResult

    /** HU-04: acceso con código y PIN, con bloqueo de 60 s tras 5 fallos consecutivos. */
    suspend fun loginLocal(institutionalCode: String, pin: String): LocalAuthResult

    /** Código del perfil local del dispositivo, o null si no hay (para precargar el acceso). */
    suspend fun getLocalProfileCode(): String?
}
