package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.CrisisSimDatabase
import com.example.crissim.data.local.dao.MicroTrainingDao
import com.example.crissim.data.repository.MicroTrainingRepository
import com.example.crissim.domain.model.Badge
import com.example.crissim.domain.model.BadgeCategory
import com.example.crissim.domain.model.MicroTraining
import com.example.crissim.domain.model.MicroTrainingStatus
import com.example.crissim.domain.model.MicroTrainingWithProgress
import com.example.crissim.domain.model.Streak
import com.example.crissim.domain.model.UserBadge
import com.example.crissim.domain.model.UserMicroTraining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación del repositorio de micro-entrenamientos (HU-24).
 */
class MicroTrainingRepositoryImpl(
    private val database: CrisisSimDatabase,
    private val microTrainingDao: MicroTrainingDao
) : MicroTrainingRepository {

    override suspend fun insertMicroTraining(microTraining: MicroTraining): Long =
        withContext(Dispatchers.IO) { microTrainingDao.insertMicroTraining(microTraining) }

    override suspend fun insertMicroTrainings(microTrainings: List<MicroTraining>) =
        withContext(Dispatchers.IO) { microTrainingDao.insertMicroTrainings(microTrainings) }

    override suspend fun getMicroTrainingById(id: Long): MicroTraining? =
        withContext(Dispatchers.IO) { microTrainingDao.getMicroTrainingById(id) }

    override suspend fun getMicroTrainingsByProtocol(protocolId: String): List<MicroTraining> =
        withContext(Dispatchers.IO) { microTrainingDao.getMicroTrainingsByProtocol(protocolId) }

    override suspend fun getAllActiveMicroTrainings(): List<MicroTraining> =
        withContext(Dispatchers.IO) { microTrainingDao.getAllActiveMicroTrainings() }

    override suspend fun getMicroTrainingsByDifficulty(difficulty: String): List<MicroTraining> =
        withContext(Dispatchers.IO) { microTrainingDao.getMicroTrainingsByDifficulty(difficulty) }

    override suspend fun getMicroTrainingsByTag(tag: String): List<MicroTraining> =
        withContext(Dispatchers.IO) { microTrainingDao.getMicroTrainingsByTag(tag) }

    override suspend fun getMicroTrainingsWithProgress(userId: Long): List<MicroTrainingWithProgress> =
        withContext(Dispatchers.IO) {
            val trainings = microTrainingDao.getAllActiveMicroTrainings()
            val userProgressMap = microTrainingDao.getUserMicroTrainings(userId).associateBy { it.microTrainingId }
            trainings.map { mt ->
                MicroTrainingWithProgress(mt, userProgressMap[mt.id])
            }
        }

    override suspend fun getMicroTrainingsByProtocolWithProgress(userId: Long, protocolId: String): List<MicroTrainingWithProgress> =
        withContext(Dispatchers.IO) {
            val trainings = microTrainingDao.getMicroTrainingsByProtocol(protocolId)
            val userProgressMap = microTrainingDao.getUserMicroTrainings(userId).associateBy { it.microTrainingId }
            trainings.map { mt ->
                MicroTrainingWithProgress(mt, userProgressMap[mt.id])
            }
        }

    override suspend fun getDueForReview(userId: Long, currentTime: Long): List<MicroTrainingWithProgress> =
        withContext(Dispatchers.IO) {
            val allWithProgress = getMicroTrainingsWithProgress(userId)
            allWithProgress.filter { item ->
                val progress = item.userProgress
                if (progress == null) {
                    true
                } else {
                    progress.status != MicroTrainingStatus.MASTERED && progress.sm2Data.nextReviewDate <= currentTime
                }
            }
        }

    override suspend fun saveUserMicroTraining(userMicroTraining: UserMicroTraining): Long =
        withContext(Dispatchers.IO) { microTrainingDao.insertUserMicroTraining(userMicroTraining) }

    override suspend fun getUserMicroTraining(userId: Long, microTrainingId: Long): UserMicroTraining? =
        withContext(Dispatchers.IO) { microTrainingDao.getUserMicroTraining(userId, microTrainingId) }

    override suspend fun getUserMicroTrainings(userId: Long): List<UserMicroTraining> =
        withContext(Dispatchers.IO) { microTrainingDao.getUserMicroTrainings(userId) }

    override suspend fun getUserMicroTrainingsByStatus(userId: Long, status: String): List<UserMicroTraining> =
        withContext(Dispatchers.IO) { microTrainingDao.getUserMicroTrainingsByStatus(userId, status) }

    // --- Badge ---

    override suspend fun getAllBadges(): List<Badge> =
        withContext(Dispatchers.IO) { microTrainingDao.getAllBadges() }

    override suspend fun getBadgeById(id: String): Badge? =
        withContext(Dispatchers.IO) { microTrainingDao.getBadgeById(id) }

    override suspend fun getBadgesByCategory(category: BadgeCategory): List<Badge> =
        withContext(Dispatchers.IO) { microTrainingDao.getBadgesByCategory(category.name) }

    override suspend fun getEarnedBadges(): List<Badge> =
        withContext(Dispatchers.IO) { microTrainingDao.getEarnedBadges() }

    override suspend fun getUserEarnedBadges(userId: Long): List<Badge> =
        withContext(Dispatchers.IO) { microTrainingDao.getUserEarnedBadges(userId) }

    override suspend fun getUnearnedBadges(userId: Long): List<Badge> =
        withContext(Dispatchers.IO) { microTrainingDao.getUnearnedBadges(userId) }

    override suspend fun insertBadge(badge: Badge): Long =
        withContext(Dispatchers.IO) { microTrainingDao.insertBadge(badge) }

    override suspend fun updateBadge(badge: Badge) =
        withContext(Dispatchers.IO) { microTrainingDao.updateBadge(badge) }

    // --- Streak ---

    override suspend fun getStreak(userId: Long): Streak? =
        withContext(Dispatchers.IO) { microTrainingDao.getStreak(userId) }

    override suspend fun saveStreak(streak: Streak) =
        withContext(Dispatchers.IO) { microTrainingDao.insertStreak(streak) }

    // --- UserBadge ---

    override suspend fun insertUserBadge(userBadge: UserBadge) =
        withContext(Dispatchers.IO) { microTrainingDao.insertUserBadge(userBadge) }

    override suspend fun getUserBadges(userId: Long): List<UserBadge> =
        withContext(Dispatchers.IO) { microTrainingDao.getUserBadges(userId) }
}
