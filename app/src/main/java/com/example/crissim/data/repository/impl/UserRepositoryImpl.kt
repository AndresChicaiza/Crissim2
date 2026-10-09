package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.dao.UserDao
import com.example.crissim.data.local.entity.UserEntity
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.domain.model.UserProfile

/**
 * Implementación del repositorio de usuario con Room.
 */
class UserRepositoryImpl(
    private val userDao: UserDao
) : UserRepository {

    override suspend fun createUser(name: String): UserProfile {
        val entity = UserEntity(name = name)
        val id = userDao.insertUser(entity)
        return UserProfile(id = id, name = name)
    }

    override suspend fun getActiveUser(): UserProfile? {
        return userDao.getActiveUser()?.toDomain()
    }

    override suspend fun updateUserName(userId: Long, name: String) {
        val existing = userDao.getUserById(userId) ?: return
        userDao.updateUser(existing.copy(name = name))
    }

    override suspend fun hasUser(): Boolean {
        return userDao.getUserCount() > 0
    }

    // ─── Mappers ───

    private fun UserEntity.toDomain(): UserProfile {
        return UserProfile(
            id = id,
            name = name,
            createdAt = createdAt
        )
    }
}
