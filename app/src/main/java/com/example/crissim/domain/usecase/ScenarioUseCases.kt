package com.example.crissim.domain.usecase

import com.example.crissim.domain.model.*
import com.example.crissim.data.repository.ScenarioRepository

/**
 * Caso de uso: cargar escenarios disponibles para un nivel.
 */
class GetScenariosUseCase(
    private val scenarioRepository: ScenarioRepository
) {
    suspend fun execute(level: Level? = null): List<Scenario> {
        val allScenarios = scenarioRepository.getAllScenarios()
        return if (level != null) {
            allScenarios.filter { it.level == level }
        } else {
            allScenarios
        }
    }
}

/**
 * Caso de uso: obtener un escenario específico por ID.
 */
class GetScenarioByIdUseCase(
    private val scenarioRepository: ScenarioRepository
) {
    suspend fun execute(scenarioId: String): Scenario? {
        return scenarioRepository.getScenarioById(scenarioId)
    }
}

/**
 * Caso de uso: verificar si un nivel está desbloqueado (RF-04).
 */
class CheckLevelUnlockedUseCase(
    private val progressRepository: com.example.crissim.data.repository.ProgressRepository
) {
    suspend fun execute(userId: Long, level: Level): Boolean {
        if (level == Level.BASIC) return true

        val previousLevel = Level.entries
            .filter { it.order < level.order }
            .maxByOrNull { it.order }
            ?: return true

        val progress = progressRepository.getLevelProgress(userId, previousLevel)
        return progress?.isPassed == true
    }
}
