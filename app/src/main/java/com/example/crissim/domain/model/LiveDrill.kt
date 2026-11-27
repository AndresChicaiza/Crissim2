package com.example.crissim.domain.model

import java.io.Serializable

/**
 * Modelo de dominio para el Modo Simulacro en Vivo (HU-25).
 */
data class LiveDrill(
    val id: String,
    val userId: Long,
    val userName: String,
    val role: UserRole,
    val startedAt: Long,
    val completedAt: Long = 0L,
    val totalDurationSeconds: Long = 0L,
    val targetTimeSeconds: Int = 180, // Meta definida por el ILC (3 min = 180 s)
    val incidents: List<DrillIncident> = emptyList(),
    val isCompleted: Boolean = false
) : Serializable

/** Incidencia registrada durante el simulacro en vivo. */
data class DrillIncident(
    val timestamp: Long,
    val stepTitle: String,
    val note: String
) : Serializable

/** Paso de la guía en vivo según el rol (Docente vs Brigadista). */
data class DrillStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val roleAction: String
) : Serializable
