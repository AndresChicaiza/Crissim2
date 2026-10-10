package com.example.crissim.domain.model

enum class UserRole(val labelEs: String) {
    STUDENT("Estudiante"),
    TEACHER("Docente"),
    ADMINISTRATIVE("Administrativo"),
    BRIGADIST("Brigadista")
}

/**
 * Perfil local del usuario (RF-08).
 * A7: Solo nombre, rol, idioma y resultados. Sin cuenta, sin red.
 */
data class UserProfile(
    val id: Long = 0,
    val name: String,
    val role: UserRole = UserRole.STUDENT,
    val language: String = "ES",
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Progreso del usuario en un nivel específico (RF-04, RF-05).
 */
data class LevelProgress(
    val level: Level,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val bestScore: Int,
    val maxScore: Int,
    val attempts: Int,
    val lastAttemptAt: Long? = null
) {
    val scorePercentage: Float
        get() = if (maxScore > 0) bestScore.toFloat() / maxScore else 0f

    /** "Completado" ya se calcula con la política del escenario (passing_score), no hay umbral aparte. */
    val isPassed: Boolean
        get() = isCompleted
}

/**
 * Resultado de un intento de escenario.
 */
data class AttemptResult(
    val id: Long = 0,
    val scenarioId: String,
    val userId: Long,
    val score: Int,
    val maxScore: Int,
    val isPassed: Boolean,
    val decisions: List<DecisionRecord>,
    val startedAt: Long,
    val completedAt: Long,
    val endNodeId: String,
    val endType: EndType
) {
    val durationSeconds: Long
        get() = (completedAt - startedAt) / 1000
}

/**
 * Registro de una decisión tomada durante un intento.
 */
data class DecisionRecord(
    val nodeId: String,
    val optionId: String,
    val timeToDecideMs: Long,
    val wasTimeout: Boolean,
    val scoreChange: Int
)
