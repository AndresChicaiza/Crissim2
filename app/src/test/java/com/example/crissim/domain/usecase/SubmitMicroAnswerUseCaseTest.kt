package com.example.crissim.domain.usecase

import com.example.crissim.data.repository.MicroTrainingRepository
import com.example.crissim.domain.model.Badge
import com.example.crissim.domain.model.BadgeCategory
import com.example.crissim.domain.model.DifficultyLevel
import com.example.crissim.domain.model.MicroOption
import com.example.crissim.domain.model.MicroTraining
import com.example.crissim.domain.model.MicroTrainingStatus
import com.example.crissim.domain.model.MicroTrainingWithProgress
import com.example.crissim.domain.model.Streak
import com.example.crissim.domain.model.UserBadge
import com.example.crissim.domain.model.UserMicroTraining
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SubmitMicroAnswerUseCaseTest {

    private lateinit var fakeRepository: FakeMicroTrainingRepository
    private lateinit var useCase: SubmitMicroAnswerUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeMicroTrainingRepository()
        useCase = SubmitMicroAnswerUseCase(fakeRepository)

        // Seed initial badge
        fakeRepository.badges["badge_first_step"] = Badge(
            id = "badge_first_step",
            name = "Primer Paso",
            description = "Completaste tu primer entrenamiento",
            iconRes = "star",
            category = BadgeCategory.CONSISTENCY,
            requirement = "1 entrenamiento"
        )
        fakeRepository.badges["badge_streak_3"] = Badge(
            id = "badge_streak_3",
            name = "Racha de 3 Días",
            description = "3 días consecutivos",
            iconRes = "fire",
            category = BadgeCategory.STREAK,
            requirement = "3 días"
        )

        // Seed initial micro-training
        fakeRepository.trainings[1L] = MicroTraining(
            id = 1L,
            protocolId = "SIS-01",
            title = "Sismo",
            question = "¿Qué hacer en sismo?",
            options = listOf(
                MicroOption(text = "Correr", isCorrect = false),
                MicroOption(text = "Cubrirse", isCorrect = true)
            ),
            correctOptionIndex = 1,
            explanation = "Debes cubrirte",
            protocolSource = "Manual",
            difficulty = DifficultyLevel.BASIC
        )
    }

    @Test
    fun submitCorrectAnswer_calculatesSm2AndUpdatesStreakAndBadges() = runBlocking {
        val result = useCase.execute(
            userId = 100L,
            microTrainingId = 1L,
            selectedOptionIndex = 1,
            userRatingQuality = 5
        )

        assertNotNull(result)
        assertTrue(result!!.isCorrect)
        assertEquals(1, result.streak.currentStreak)
        assertEquals(1, result.streak.totalPracticeDays)
        assertTrue(result.newlyEarnedBadges.any { it.id == "badge_first_step" })
    }

    @Test
    fun submitIncorrectAnswer_resetsRepetitionsInSm2AndDoesNotEarnBadges() = runBlocking {
        val result = useCase.execute(
            userId = 100L,
            microTrainingId = 1L,
            selectedOptionIndex = 0
        )

        assertNotNull(result)
        assertEquals(false, result!!.isCorrect)
        assertEquals(0, result.updatedSm2Data.repetitions)
        assertEquals(1, result.updatedSm2Data.interval)
        assertEquals(0, result.streak.currentStreak)
        assertTrue(result.newlyEarnedBadges.isEmpty())
    }
}

class FakeMicroTrainingRepository : MicroTrainingRepository {
    val trainings = mutableMapOf<Long, MicroTraining>()
    val userProgressMap = mutableMapOf<Pair<Long, Long>, UserMicroTraining>()
    val streaks = mutableMapOf<Long, Streak>()
    val badges = mutableMapOf<String, Badge>()
    val userBadgesMap = mutableListOf<UserBadge>()

    override suspend fun insertMicroTraining(microTraining: MicroTraining): Long {
        trainings[microTraining.id] = microTraining
        return microTraining.id
    }

    override suspend fun insertMicroTrainings(microTrainings: List<MicroTraining>) {
        microTrainings.forEach { insertMicroTraining(it) }
    }

    override suspend fun getMicroTrainingById(id: Long): MicroTraining? = trainings[id]

    override suspend fun getMicroTrainingsByProtocol(protocolId: String): List<MicroTraining> =
        trainings.values.filter { it.protocolId == protocolId }

    override suspend fun getAllActiveMicroTrainings(): List<MicroTraining> =
        trainings.values.filter { it.isActive }

    override suspend fun getMicroTrainingsByDifficulty(difficulty: String): List<MicroTraining> =
        trainings.values.filter { it.difficulty.name == difficulty }

    override suspend fun getMicroTrainingsByTag(tag: String): List<MicroTraining> =
        trainings.values.filter { tag in it.tags }

    override suspend fun getMicroTrainingsWithProgress(userId: Long): List<MicroTrainingWithProgress> =
        trainings.values.map { mt ->
            MicroTrainingWithProgress(mt, userProgressMap[userId to mt.id])
        }

    override suspend fun getMicroTrainingsByProtocolWithProgress(userId: Long, protocolId: String): List<MicroTrainingWithProgress> =
        trainings.values.filter { it.protocolId == protocolId }.map { mt ->
            MicroTrainingWithProgress(mt, userProgressMap[userId to mt.id])
        }

    override suspend fun getDueForReview(userId: Long, currentTime: Long): List<MicroTrainingWithProgress> =
        getMicroTrainingsWithProgress(userId).filter {
            val progress = it.userProgress
            progress == null || (progress.status != MicroTrainingStatus.MASTERED && progress.sm2Data.nextReviewDate <= currentTime)
        }

    override suspend fun saveUserMicroTraining(userMicroTraining: UserMicroTraining): Long {
        userProgressMap[userMicroTraining.userId to userMicroTraining.microTrainingId] = userMicroTraining
        return userMicroTraining.id
    }

    override suspend fun getUserMicroTraining(userId: Long, microTrainingId: Long): UserMicroTraining? =
        userProgressMap[userId to microTrainingId]

    override suspend fun getUserMicroTrainings(userId: Long): List<UserMicroTraining> =
        userProgressMap.filterKeys { it.first == userId }.values.toList()

    override suspend fun getUserMicroTrainingsByStatus(userId: Long, status: String): List<UserMicroTraining> =
        getUserMicroTrainings(userId).filter { it.status.name == status }

    override suspend fun getAllBadges(): List<Badge> = badges.values.toList()

    override suspend fun getBadgeById(id: String): Badge? = badges[id]

    override suspend fun getBadgesByCategory(category: BadgeCategory): List<Badge> =
        badges.values.filter { it.category == category }

    override suspend fun getEarnedBadges(): List<Badge> = badges.values.filter { it.earnedAt > 0 }

    override suspend fun getUserEarnedBadges(userId: Long): List<Badge> {
        val earnedIds = userBadgesMap.filter { it.userId == userId }.map { it.badgeId }
        return badges.values.filter { it.id in earnedIds }
    }

    override suspend fun getUnearnedBadges(userId: Long): List<Badge> {
        val earnedIds = userBadgesMap.filter { it.userId == userId }.map { it.badgeId }
        return badges.values.filter { it.id !in earnedIds }
    }

    override suspend fun insertBadge(badge: Badge): Long {
        badges[badge.id] = badge
        return 1L
    }

    override suspend fun updateBadge(badge: Badge) {
        badges[badge.id] = badge
    }

    override suspend fun getStreak(userId: Long): Streak? = streaks[userId]

    override suspend fun saveStreak(streak: Streak) {
        streaks[streak.userId] = streak
    }

    override suspend fun insertUserBadge(userBadge: UserBadge) {
        userBadgesMap.add(userBadge)
    }

    override suspend fun getUserBadges(userId: Long): List<UserBadge> =
        userBadgesMap.filter { it.userId == userId }
}
