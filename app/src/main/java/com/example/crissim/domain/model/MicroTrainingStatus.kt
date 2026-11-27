package com.example.crissim.domain.model

/**
 * Estado del progreso del usuario en un micro-entrenamiento.
 */
enum class MicroTrainingStatus {
    PENDING,      // Pendiente de hacer
    COMPLETED,    // Completado al menos una vez
    MASTERED      // Dominado (intervalo SM-2 > umbral, ej: > 30 días)
}