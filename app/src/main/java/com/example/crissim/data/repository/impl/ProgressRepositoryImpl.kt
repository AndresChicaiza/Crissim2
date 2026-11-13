package com.example.crissim.data.repository.impl

import androidx.room.withTransaction
import com.example.crissim.data.local.CrisisSimDatabase
import com.example.crissim.data.local.dao.AttemptDao
import com.example.crissim.data.local.dao.LevelProgressDao
import com.example.crissim.data.local.dao.ScenarioProgressBackfillDao
import com.example.crissim.data.local.dao.ScenarioProgressDao
import com.example.crissim.data.local.entity.AttemptEntity
import com.example.crissim.data.local.entity.ScenarioProgressBackfillEntity
import com.example.crissim.data.local.entity.ScenarioProgressEntity
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.domain.model.*
import com.google.gson.Gson

/** Persists attempt history and derives level completion from the required scenario catalog. */
class ProgressRepositoryImpl(
    private val database: CrisisSimDatabase,
    private val attemptDao: AttemptDao,
    private val levelProgressDao: LevelProgressDao,
    private val scenarioProgressDao: ScenarioProgressDao,
    private val backfillDao: ScenarioProgressBackfillDao,
    private val scenarioRepository: ScenarioRepository,
    private val gson: Gson = Gson()
) : ProgressRepository {

    override suspend fun getLevelProgress(userId: Long, level: Level): LevelProgress? {
        ensureBackfilled(userId)
        val scenarios = scenarioRepository.getAllScenarios().filter { it.level == level }
        val rows = scenarioProgressDao.getByLevel(userId, level.name)
        val byId = rows.associateBy { it.scenarioId }
        val attempts = attemptDao.getAllAttempts(userId).filter { attempt ->
            scenarios.any { it.id == attempt.scenarioId }
        }
        val legacy = levelProgressDao.getProgress(userId, level.name)
        val totalBest = rows.sumOf { it.bestScore }
        val totalMax = scenarios.sumOf { it.calculateMaxScore() }
        return LevelProgress(
            level = level,
            isUnlocked = level == Level.BASIC || getLevelProgress(userId, Level.entries[level.ordinal - 1])?.isCompleted == true,
            isCompleted = ScenarioProgressPolicy.isLevelComplete(scenarios, byId),
            bestScore = if (rows.isEmpty()) legacy?.bestScore ?: 0 else totalBest,
            maxScore = if (totalMax > 0) totalMax else legacy?.maxScore ?: 0,
            attempts = attempts.size,
            lastAttemptAt = attempts.maxOfOrNull { it.completedAt } ?: legacy?.lastAttemptAt
        )
    }

    override suspend fun getAllLevelProgress(userId: Long): List<LevelProgress> =
        Level.entries.mapNotNull { getLevelProgress(userId, it) }

    override suspend fun saveAttemptResult(result: AttemptResult) {
        attemptDao.insertAttempt(result.toEntity())
    }

    override suspend fun recordAttemptResult(result: AttemptResult) {
        val scenario = scenarioRepository.getScenarioById(result.scenarioId)
        database.withTransaction {
            ensureBackfilledInTransaction(result.userId)
            val inserted = attemptDao.insertAttempt(result.toEntity())
            if (inserted != -1L && scenario != null) {
                val previous = scenarioProgressDao.get(result.userId, scenario.id)
                scenarioProgressDao.upsert(ScenarioProgressReducer.merge(previous, result, scenario))
            }
        }
    }

    override suspend fun updateLevelProgress(result: AttemptResult) {
        val scenario = scenarioRepository.getScenarioById(result.scenarioId) ?: return
        database.withTransaction {
            ensureBackfilledInTransaction(result.userId)
            val previous = scenarioProgressDao.get(result.userId, scenario.id)
            scenarioProgressDao.upsert(ScenarioProgressReducer.merge(previous, result, scenario))
        }
    }

    private suspend fun ensureBackfilled(userId: Long) {
        if (backfillDao.isComplete(userId)) return
        val catalog = scenarioRepository.getAllScenarios().associateBy { it.id }
        database.withTransaction { backfillUser(userId, catalog) }
    }

    private suspend fun ensureBackfilledInTransaction(userId: Long) {
        if (backfillDao.isComplete(userId)) return
        val catalog = scenarioRepository.getAllScenarios().associateBy { it.id }
        backfillUser(userId, catalog)
    }

    private suspend fun backfillUser(userId: Long, catalog: Map<String, Scenario>) {
        if (backfillDao.isComplete(userId)) return
        val grouped = attemptDao.getAllAttempts(userId).groupBy { it.scenarioId }
        grouped.forEach { (scenarioId, entities) ->
            val scenario = catalog[scenarioId] ?: return@forEach
            var aggregate = scenarioProgressDao.get(userId, scenarioId)
            entities.sortedBy { it.completedAt }.forEach { entity ->
                aggregate = ScenarioProgressReducer.merge(aggregate, entity.toDomain(), scenario)
            }
            aggregate?.let { scenarioProgressDao.upsert(it) }
        }
        // Legacy level_progress is deliberately retained verbatim. It cannot prove that every
        // currently required scenario was completed, so it is never used to infer completion.
        backfillDao.markComplete(ScenarioProgressBackfillEntity(userId))
    }

    override suspend fun getAttemptHistory(userId: Long, scenarioId: String): List<AttemptResult> =
        attemptDao.getAttemptsByScenario(userId, scenarioId).map { it.toDomain() }

    override suspend fun getAllAttemptHistory(userId: Long): List<AttemptResult> =
        attemptDao.getAllAttempts(userId).map { it.toDomain() }

    private fun AttemptResult.toEntity() = AttemptEntity(
        attemptUuid = attemptUuid,
        scenarioId = scenarioId,
        userId = userId,
        score = score,
        maxScore = maxScore,
        isPassed = isPassed,
        startedAt = startedAt,
        completedAt = completedAt,
        endNodeId = endNodeId,
        endType = endType.name,
        decisionsJson = gson.toJson(decisions),
        scenarioVersion = scenarioVersion
    )

    private fun AttemptEntity.toDomain(): AttemptResult {
        val decisions = try {
            gson.fromJson(decisionsJson, Array<DecisionRecord>::class.java).toList()
        } catch (_: Exception) { emptyList() }
        return AttemptResult(
            id = id, scenarioId = scenarioId, userId = userId, score = score,
            maxScore = maxScore, isPassed = isPassed, decisions = decisions,
            startedAt = startedAt, completedAt = completedAt, endNodeId = endNodeId,
            endType = runCatching { EndType.valueOf(endType) }.getOrDefault(EndType.SUCCESS),
            attemptUuid = attemptUuid, scenarioVersion = scenarioVersion
        )
    }
}
