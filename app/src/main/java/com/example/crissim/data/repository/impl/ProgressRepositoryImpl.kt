package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.dao.AttemptDao
import com.example.crissim.data.local.dao.LevelProgressDao
import com.example.crissim.data.local.entity.AttemptEntity
import com.example.crissim.data.local.entity.LevelProgressEntity
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.domain.model.*
import com.google.gson.Gson

/**
 * Implementación del repositorio de progreso con Room.
 */
class ProgressRepositoryImpl(
    private val attemptDao: AttemptDao,
    private val levelProgressDao: LevelProgressDao,
    private val gson: Gson = Gson()
) : ProgressRepository {

    override suspend fun getLevelProgress(userId: Long, level: Level): LevelProgress? {
        val entity = levelProgressDao.getProgress(userId, level.name) ?: return null
        return entity.toDomain(level)
    }

    override suspend fun getAllLevelProgress(userId: Long): List<LevelProgress> {
        return levelProgressDao.getAllProgress(userId).map { entity ->
            entity.toDomain(Level.valueOf(entity.level))
        }
    }

    override suspend fun saveAttemptResult(result: AttemptResult) {
        val entity = AttemptEntity(
            scenarioId = result.scenarioId,
            userId = result.userId,
            score = result.score,
            maxScore = result.maxScore,
            isPassed = result.isPassed,
            startedAt = result.startedAt,
            completedAt = result.completedAt,
            endNodeId = result.endNodeId,
            endType = result.endType.name,
            decisionsJson = gson.toJson(result.decisions)
        )
        attemptDao.insertAttempt(entity)
    }

    override suspend fun updateLevelProgress(result: AttemptResult) {
        // Determinar el nivel del escenario
        // Por ahora se infiere del scenarioId, pero se podría almacenar directamente
        val level = inferLevelFromScenarioId(result.scenarioId)
        val existing = levelProgressDao.getProgress(result.userId, level.name)

        if (existing == null) {
            // Crear nuevo registro de progreso
            val entity = LevelProgressEntity(
                userId = result.userId,
                level = level.name,
                isCompleted = result.isPassed,
                bestScore = result.score,
                maxScore = result.maxScore,
                attempts = 1,
                lastAttemptAt = result.completedAt
            )
            levelProgressDao.insertOrUpdate(entity)
        } else {
            // Actualizar solo si el score es mejor
            val newBestScore = maxOf(existing.bestScore, result.score)
            val newCompleted = existing.isCompleted || result.isPassed
            levelProgressDao.updateProgress(
                userId = result.userId,
                level = level.name,
                score = newBestScore,
                completed = newCompleted,
                timestamp = result.completedAt
            )
        }
    }

    override suspend fun getAttemptHistory(userId: Long, scenarioId: String): List<AttemptResult> {
        return attemptDao.getAttemptsByScenario(userId, scenarioId).map { entity ->
            entity.toDomain()
        }
    }

    // ─── Mappers ───

    private fun LevelProgressEntity.toDomain(level: Level): LevelProgress {
        return LevelProgress(
            level = level,
            isUnlocked = true, // Se calcula en el use case
            isCompleted = isCompleted,
            bestScore = bestScore,
            maxScore = maxScore,
            attempts = attempts,
            lastAttemptAt = lastAttemptAt
        )
    }

    private fun AttemptEntity.toDomain(): AttemptResult {
        val decisions: List<DecisionRecord> = try {
            gson.fromJson(
                decisionsJson,
                Array<DecisionRecord>::class.java
            ).toList()
        } catch (e: Exception) {
            emptyList()
        }

        return AttemptResult(
            id = id,
            scenarioId = scenarioId,
            userId = userId,
            score = score,
            maxScore = maxScore,
            isPassed = isPassed,
            decisions = decisions,
            startedAt = startedAt,
            completedAt = completedAt,
            endNodeId = endNodeId,
            endType = try {
                EndType.valueOf(endType)
            } catch (e: Exception) {
                EndType.SUCCESS
            }
        )
    }

    private fun inferLevelFromScenarioId(scenarioId: String): Level {
        return when {
            scenarioId.contains("basic", ignoreCase = true) -> Level.BASIC
            scenarioId.contains("intermediate", ignoreCase = true) -> Level.INTERMEDIATE
            scenarioId.contains("advanced", ignoreCase = true) -> Level.ADVANCED
            else -> Level.BASIC
        }
    }
}
