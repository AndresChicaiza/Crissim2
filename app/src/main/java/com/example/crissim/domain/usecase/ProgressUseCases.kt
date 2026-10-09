package com.example.crissim.domain.usecase

import com.example.crissim.domain.model.*
import com.example.crissim.data.repository.ProgressRepository

/**
 * Caso de uso: guardar el resultado de un intento y actualizar el progreso (RF-05).
 */
class SaveAttemptResultUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend fun execute(result: AttemptResult) {
        progressRepository.saveAttemptResult(result)
        progressRepository.updateLevelProgress(result)
    }
}

/**
 * Caso de uso: obtener el progreso de todos los niveles para un usuario.
 */
class GetAllProgressUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend fun execute(userId: Long): List<LevelProgress> {
        return Level.entries.map { level ->
            progressRepository.getLevelProgress(userId, level) ?: LevelProgress(
                level = level,
                isUnlocked = level == Level.BASIC,
                isCompleted = false,
                bestScore = 0,
                maxScore = 0,
                attempts = 0
            )
        }
    }
}

/**
 * Caso de uso: obtener el historial de intentos de un escenario.
 */
class GetAttemptHistoryUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend fun execute(userId: Long, scenarioId: String): List<AttemptResult> {
        return progressRepository.getAttemptHistory(userId, scenarioId)
    }
}
