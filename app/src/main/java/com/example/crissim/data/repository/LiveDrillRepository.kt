package com.example.crissim.data.repository

import com.example.crissim.domain.model.LiveDrill

/**
 * Repositorio para guardar y consultar los informes de Simulacro en Vivo (HU-25).
 */
interface LiveDrillRepository {
    suspend fun saveDrill(drill: LiveDrill)
    suspend fun getDrillById(id: String): LiveDrill?
    suspend fun getDrillsForUser(userId: Long): List<LiveDrill>
}
