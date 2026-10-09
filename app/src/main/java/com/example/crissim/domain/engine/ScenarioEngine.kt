package com.example.crissim.domain.engine

import com.example.crissim.domain.model.*

/**
 * Motor de escenarios ramificados (RF-01, RF-02, A2).
 *
 * Es una máquina de estados que recorre el grafo dirigido del escenario.
 * Kotlin puro, sin dependencias Android → 100 % testeable.
 *
 * Responsabilidades:
 * - Cargar un escenario y posicionarse en el nodo inicial o en uno guardado (RF-10).
 * - Procesar decisiones del usuario y calcular puntaje.
 * - Manejar timeout con opción por defecto (RF-02).
 * - Generar registros de cada decisión para el resultado final.
 * - Determinar si el escenario terminó y con qué tipo de final.
 */
class ScenarioEngine {

    private var scenario: Scenario? = null
    private var currentNode: ScenarioNode? = null
    private var score: Int = 0
    private var decisions: MutableList<DecisionRecord> = mutableListOf()
    private var startTime: Long = 0L
    private var nodeEntryTime: Long = 0L
    private var isFinished: Boolean = false

    /** Estado actual del motor, expuesto como snapshot inmutable. */
    val state: EngineState
        get() = EngineState(
            scenarioId = scenario?.id,
            currentNode = currentNode,
            score = score,
            decisions = decisions.toList(),
            isFinished = isFinished,
            startTime = startTime
        )

    /**
     * Inicia un escenario nuevo desde el nodo inicial.
     */
    fun startScenario(scenario: Scenario) {
        this.scenario = scenario
        this.score = 0
        this.decisions = mutableListOf()
        this.startTime = System.currentTimeMillis()
        this.isFinished = false
        navigateToNode(scenario.startNodeId)
    }

    /**
     * Reanuda un escenario interrumpido (RF-10, A3).
     * Restaura el estado guardado: nodo actual, puntaje y decisiones.
     */
    fun resumeScenario(
        scenario: Scenario,
        savedState: SavedEngineState
    ) {
        this.scenario = scenario
        this.score = savedState.score
        this.decisions = savedState.decisions.toMutableList()
        this.startTime = savedState.startTime
        this.isFinished = false
        navigateToNode(savedState.currentNodeId)
    }

    /**
     * Procesa la decisión del usuario en el nodo actual.
     * Retorna el feedback de la opción elegida, o null si no es válida.
     */
    fun selectOption(optionId: String): OptionResult? {
        val node = currentNode ?: return null
        if (isFinished) return null

        val option = node.options.find { it.id == optionId } ?: return null
        return processOption(node, option, wasTimeout = false)
    }

    /**
     * Procesa el timeout (RF-02).
     * Aplica la opción por defecto definida en el nodo.
     * Si no hay opción por defecto, se queda en el mismo nodo.
     */
    fun handleTimeout(): OptionResult? {
        val node = currentNode ?: return null
        if (isFinished) return null

        val defaultIndex = node.defaultOptionIndex ?: return null
        val option = node.options.getOrNull(defaultIndex) ?: return null
        return processOption(node, option, wasTimeout = true)
    }

    /**
     * Obtiene el estado para guardar en Room (A3).
     * Se llama en cada transición de nodo para garantizar reanudación.
     */
    fun getSaveState(): SavedEngineState? {
        val s = scenario ?: return null
        val n = currentNode ?: return null
        return SavedEngineState(
            scenarioId = s.id,
            currentNodeId = n.id,
            score = score,
            decisions = decisions.toList(),
            startTime = startTime
        )
    }

    /**
     * Genera el resultado final del intento.
     * Solo se puede llamar cuando el escenario ha terminado.
     */
    fun getAttemptResult(userId: Long): AttemptResult? {
        if (!isFinished) return null
        val s = scenario ?: return null
        val n = currentNode ?: return null

        val maxScore = s.calculateMaxScore()
        val percent = if (maxScore > 0) score * 100 / maxScore else 0

        return AttemptResult(
            scenarioId = s.id,
            userId = userId,
            score = score,
            maxScore = maxScore,
            isPassed = percent >= s.passingScore, // passingScore = porcentaje (0-100)
            decisions = decisions.toList(),
            startedAt = startTime,
            completedAt = System.currentTimeMillis(),
            endNodeId = n.id,
            endType = n.endType ?: EndType.SUCCESS
        )
    }

    // ─── Funciones internas ───────────────────────────────

    private fun processOption(
        node: ScenarioNode,
        option: NodeOption,
        wasTimeout: Boolean
    ): OptionResult {
        val timeToDecide = System.currentTimeMillis() - nodeEntryTime

        // Registrar la decisión
        val record = DecisionRecord(
            nodeId = node.id,
            optionId = option.id,
            timeToDecideMs = timeToDecide,
            wasTimeout = wasTimeout,
            scoreChange = option.scoreChange
        )
        decisions.add(record)

        // Actualizar puntaje
        score += option.scoreChange

        // Navegar al siguiente nodo
        navigateToNode(option.nextNodeId)

        return OptionResult(
            feedback = option.feedback,
            scoreChange = option.scoreChange,
            wasTimeout = wasTimeout,
            isScenarioEnd = isFinished,
            endType = if (isFinished) currentNode?.endType else null,
            finalFeedback = if (isFinished) currentNode?.feedback else null
        )
    }

    private fun navigateToNode(nodeId: String) {
        val s = scenario ?: return
        val node = s.getNode(nodeId)
        currentNode = node
        nodeEntryTime = System.currentTimeMillis()

        if (node?.isEnd == true) {
            isFinished = true
        }
    }
}
