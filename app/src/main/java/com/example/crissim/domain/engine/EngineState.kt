package com.example.crissim.domain.engine

import com.example.crissim.domain.model.*

/**
 * Estado actual del motor, expuesto como snapshot inmutable
 * para la capa de presentación.
 */
data class EngineState(
    val scenarioId: String?,
    val currentNode: ScenarioNode?,
    val score: Int,
    val decisions: List<DecisionRecord>,
    val isFinished: Boolean,
    val startTime: Long
)

/**
 * Estado persistible del motor (A3).
 * Se guarda en Room en cada transición de nodo para permitir reanudación (RF-10).
 */
data class SavedEngineState(
    val scenarioId: String,
    val currentNodeId: String,
    val score: Int,
    val decisions: List<DecisionRecord>,
    val startTime: Long
)

/**
 * Resultado de procesar una opción/decisión.
 * Contiene el feedback y la información de transición.
 */
data class OptionResult(
    val feedback: Feedback?,
    val scoreChange: Int,
    val wasTimeout: Boolean,
    val isScenarioEnd: Boolean,
    val endType: EndType?,
    val finalFeedback: Feedback?
)
