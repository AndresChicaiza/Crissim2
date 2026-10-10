package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.dao.UserDao
import com.example.crissim.data.local.entity.UserEntity
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.domain.model.UserProfile
import com.example.crissim.domain.model.UserRole

/**
 * Implementación del repositorio de usuario con Room.
 */
class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun createUser(name: String, role: UserRole, language: String): UserProfile {
        val entity = UserEntity(name = name, role = role.name, language = language)
        val id = userDao.insertUser(entity)
        return UserProfile(id = id, name = name, role = role, language = language)
    }

    override suspend fun getActiveUser(): UserProfile? {
        return userDao.getActiveUser()?.toDomain()
    }

    override suspend fun updateUserName(userId: Long, name: String) {
        val existing = userDao.getUserById(userId) ?: return
        userDao.updateUser(existing.copy(name = name))
    }

    override suspend fun updateUserRole(userId: Long, role: UserRole) {
        val existing = userDao.getUserById(userId) ?: return
        userDao.updateUser(existing.copy(role = role.name))
    }

    override suspend fun updateUserLanguage(userId: Long, language: String) {
        val existing = userDao.getUserById(userId) ?: return
        userDao.updateUser(existing.copy(language = language))
    }

    override suspend fun hasUser(): Boolean {
        return userDao.getUserCount() > 0
    }

    // ─── Mappers ───

    private fun UserEntity.toDomain(): UserProfile {
        val parsedRole = try {
            UserRole.valueOf(role)
        } catch (e: Exception) {
            UserRole.STUDENT
        }
        return UserProfile(
            id = id,
            name = name,
            role = parsedRole,
            language = language,
            createdAt = createdAt
        )
    }
}
