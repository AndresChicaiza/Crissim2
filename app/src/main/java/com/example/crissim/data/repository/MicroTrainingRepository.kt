package com.example.crissim.data.repository

import com.example.crissim.domain.model.Badge
import com.example.crissim.domain.model.BadgeCategory
import com.example.crissim.domain.model.MicroTraining
import com.example.crissim.domain.model.MicroTrainingWithProgress
import com.example.crissim.domain.model.Streak
import com.example.crissim.domain.model.UserBadge
import com.example.crissim.domain.model.UserMicroTraining

/**
 * Repositorio para micro-entrenamientos, insignias y rachas (HU-24).
 */
interface MicroTrainingRepository {

    // --- MicroTraining ---
    suspend fun insertMicroTraining(microTraining: MicroTraining): Long
    suspend fun insertMicroTrainings(microTrainings: List<MicroTraining>)
    suspend fun getMicroTrainingById(id: Long): MicroTraining?
    suspend fun getMicroTrainingsByProtocol(protocolId: String): List<MicroTraining>
    suspend fun getAllActiveMicroTrainings(): List<MicroTraining>
    suspend fun getMicroTrainingsByDifficulty(difficulty: String): List<MicroTraining>
    suspend fun getMicroTrainingsByTag(tag: String): List<MicroTraining>
    suspend fun getMicroTrainingsWithProgress(userId: Long): List<MicroTrainingWithProgress>
    suspend fun getMicroTrainingsByProtocolWithProgress(userId: Long, protocolId: String): List<MicroTrainingWithProgress>
    suspend fun getDueForReview(userId: Long, currentTime: Long): List<MicroTrainingWithProgress>

    // --- UserMicroTraining ---
    suspend fun saveUserMicroTraining(userMicroTraining: UserMicroTraining): Long
    suspend fun getUserMicroTraining(userId: Long, microTrainingId: Long): UserMicroTraining?
    suspend fun getUserMicroTrainings(userId: Long): List<UserMicroTraining>
    suspend fun getUserMicroTrainingsByStatus(userId: Long, status: String): List<UserMicroTraining>

    // --- Badge ---
    suspend fun getAllBadges(): List<Badge>
    suspend fun getBadgeById(id: String): Badge?
    suspend fun getBadgesByCategory(category: BadgeCategory): List<Badge>
    suspend fun getEarnedBadges(): List<Badge>
    suspend fun getUserEarnedBadges(userId: Long): List<Badge>
    suspend fun getUnearnedBadges(userId: Long): List<Badge>
    suspend fun insertBadge(badge: Badge): Long
    suspend fun updateBadge(badge: Badge)

    // --- Streak ---
    suspend fun getStreak(userId: Long): Streak?
    suspend fun saveStreak(streak: Streak)

    // --- UserBadge ---
    suspend fun insertUserBadge(userBadge: UserBadge)
    suspend fun getUserBadges(userId: Long): List<UserBadge>
}
