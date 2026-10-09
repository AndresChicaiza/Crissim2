package com.example.crissim.domain.model

/**
 * Representa un escenario completo de simulación.
 * A1: El contenido es datos (JSON), no código.
 * A2: El escenario es un grafo dirigido de nodos y opciones.
 */
data class Scenario(
    val id: String,
    val version: String,
    val type: ScenarioType,
    val level: Level,
    val title: LocalizedText,
    val description: LocalizedText,
    val startNodeId: String,
    val passingScore: Int,
    val maxAttempts: Int,
    val nodes: List<ScenarioNode>
) {
    /** Obtiene un nodo por su ID. */
    fun getNode(nodeId: String): ScenarioNode? =
        nodes.find { it.id == nodeId }

    /** Obtiene todos los nodos finales del escenario. */
    fun getEndNodes(): List<ScenarioNode> =
        nodes.filter { it.isEnd }

    /**
     * Puntaje máximo posible: camino de mayor puntaje desde el nodo inicial.
     * Programación dinámica con memoización (el grafo no tiene ciclos; el validador lo exige).
     */
    fun calculateMaxScore(): Int = maxScoreFrom(startNodeId, mutableMapOf(), mutableSetOf())

    private fun maxScoreFrom(
        nodeId: String,
        memo: MutableMap<String, Int>,
        visiting: MutableSet<String>
    ): Int {
        memo[nodeId]?.let { return it }
        if (!visiting.add(nodeId)) return 0 // protección ante ciclos
        val node = getNode(nodeId)
        val best = if (node == null || node.isEnd || node.options.isEmpty()) {
            0
        } else {
            node.options.maxOf { it.scoreChange + maxScoreFrom(it.nextNodeId, memo, visiting) }
        }
        visiting.remove(nodeId)
        memo[nodeId] = best
        return best
    }
}

/**
 * Tipos de escenario soportados.
 */
enum class ScenarioType {
    EARTHQUAKE,  // Sismo
    FIRE         // Incendio
}

/**
 * Niveles de capacitación (RF-04).
 * Desbloqueo progresivo: N+1 requiere superar N.
 */
enum class Level(val order: Int) {
    BASIC(1),        // Básico: evacuación escolar e incendios
    INTERMEDIATE(2), // Intermedio: sismos
    ADVANCED(3);     // Avanzado: primeros auxilios y liderazgo

    fun isUnlockedBy(completedLevel: Level?): Boolean =
        this == BASIC || (completedLevel != null && completedLevel.order >= this.order - 1)
}
