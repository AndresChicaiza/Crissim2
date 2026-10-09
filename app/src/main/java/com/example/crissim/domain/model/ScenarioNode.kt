package com.example.crissim.domain.model

/**
 * Un nodo dentro del grafo del escenario.
 * Puede ser una situación con opciones o un nodo final.
 */
data class ScenarioNode(
    val id: String,
    val narrative: LocalizedText,
    val imageRef: String? = null,
    val timeLimitSeconds: Int? = null,
    val defaultOptionIndex: Int? = null,
    val options: List<NodeOption> = emptyList(),
    val isEnd: Boolean = false,
    val endType: EndType? = null,
    val feedback: Feedback? = null
)

/**
 * Una opción/decisión dentro de un nodo.
 * Es la arista del grafo dirigido (A2).
 */
data class NodeOption(
    val id: String,
    val text: LocalizedText,
    val nextNodeId: String,
    val scoreChange: Int = 0,
    val feedback: Feedback? = null
)

/**
 * Retroalimentación inmediata con la fuente del protocolo (RF-03).
 */
data class Feedback(
    val text: LocalizedText,
    val isCorrect: Boolean,
    val protocolSource: String,
    val protocolRef: String? = null
)

/**
 * Tipo de final del escenario.
 */
enum class EndType {
    SUCCESS,          // Evacuación exitosa
    PARTIAL_SUCCESS,  // Sobrevivió pero con errores
    FAILURE           // Consecuencia grave
}
