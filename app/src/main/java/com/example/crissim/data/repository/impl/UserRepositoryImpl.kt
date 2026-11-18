package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.dao.UserDao
import com.example.crissim.data.local.entity.UserEntity
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.domain.model.UserProfile
import com.example.crissim.domain.model.UserRole
import com.example.crissim.domain.model.UserAccountType
import com.example.crissim.domain.security.LocalAuthPolicy
import com.example.crissim.domain.security.LocalAuthResult
import com.example.crissim.domain.security.PinHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación del repositorio de usuario con Room.
 */
class UserRepositoryImpl(
    private val userDao: UserDao,
    private val clock: () -> Long = System::currentTimeMillis
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

    override suspend fun getLocalProfileCode(): String? =
        userDao.getActiveUser()?.institutionalCode

    override suspend fun registerLocal(
        name: String,
        institutionalCode: String,
        pin: String,
        role: UserRole
    ): LocalAuthResult {
        val cleanName = name.trim()
        val code = LocalAuthPolicy.normalizeCode(institutionalCode)
        val errors = LocalAuthPolicy.validateRegistration(cleanName, code, pin)
        if (errors.hasErrors) return LocalAuthResult.InvalidFields(errors)

        if (userDao.getUserByInstitutionalCode(code) != null) return LocalAuthResult.CodeAlreadyRegistered
        val existing = userDao.getActiveUser()
        if (existing != null && existing.institutionalCode != null) return LocalAuthResult.DeviceHasOtherProfile

        val salt = PinHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { PinHasher.hash(pin, salt) }
        val saved = if (existing == null) {
            val entity = UserEntity(
                name = cleanName, role = role.name,
                accountType = UserAccountType.LOCAL.name,
                institutionalCode = code, pinHash = hash, pinSalt = salt
            )
            entity.copy(id = userDao.insertUser(entity))
        } else {
            // Perfil invitado o vinculado a Firebase sin PIN: se completa sin tocar su progreso.
            val upgraded = existing.copy(
                name = cleanName, role = role.name,
                accountType = if (existing.firebaseUid != null) existing.accountType else UserAccountType.LOCAL.name,
                institutionalCode = code, pinHash = hash, pinSalt = salt,
                failedAttempts = 0, lockedUntil = 0
            )
            userDao.updateUser(upgraded)
            upgraded
        }
        return LocalAuthResult.Success(saved.toDomain())
    }

    override suspend fun loginLocal(institutionalCode: String, pin: String): LocalAuthResult {
        val code = LocalAuthPolicy.normalizeCode(institutionalCode)
        val errors = LocalAuthPolicy.validateLogin(code, pin)
        if (errors.hasErrors) return LocalAuthResult.InvalidFields(errors)

        val user = userDao.getUserByInstitutionalCode(code)
        val hash = user?.pinHash
        val salt = user?.pinSalt
        if (user == null || hash == null || salt == null) return LocalAuthResult.InvalidCredentials(attemptsLeft = null)

        val now = clock()
        val remaining = LocalAuthPolicy.remainingLockMillis(user.lockedUntil, now)
        if (remaining > 0) return LocalAuthResult.Locked(remaining)

        val ok = withContext(Dispatchers.Default) { PinHasher.verify(pin, salt, hash) }
        if (ok) {
            if (user.failedAttempts != 0 || user.lockedUntil != 0L) {
                userDao.updateUser(user.copy(failedAttempts = 0, lockedUntil = 0))
            }
            return LocalAuthResult.Success(user.toDomain())
        }

        val (failed, lockedUntil) = LocalAuthPolicy.afterFailure(user.failedAttempts, user.lockedUntil, now)
        userDao.updateUser(user.copy(failedAttempts = failed, lockedUntil = lockedUntil))
        return if (failed >= LocalAuthPolicy.MAX_FAILED_ATTEMPTS) {
            LocalAuthResult.Locked(LocalAuthPolicy.LOCK_MILLIS)
        } else {
            LocalAuthResult.InvalidCredentials(attemptsLeft = LocalAuthPolicy.MAX_FAILED_ATTEMPTS - failed)
        }
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
            createdAt = createdAt,
            accountType = runCatching { UserAccountType.valueOf(accountType) }
                .getOrDefault(UserAccountType.GUEST),
            firebaseUid = firebaseUid,
            institutionalCode = institutionalCode
        )
    }
}
