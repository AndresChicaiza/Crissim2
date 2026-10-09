package com.example.crissim.data.repository

import com.example.crissim.domain.model.*

/**
 * Repositorio de progreso del usuario.
 */
interface ProgressRepository {
    suspend fun getLevelProgress(userId: Long, level: Level): LevelProgress?
    suspend fun getAllLevelProgress(userId: Long): List<LevelProgress>
    suspend fun saveAttemptResult(result: AttemptResult)
    suspend fun updateLevelProgress(result: AttemptResult)
    suspend fun getAttemptHistory(userId: Long, scenarioId: String): List<AttemptResult>
}
