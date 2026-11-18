package com.example.crissim.data.repository

import com.example.crissim.domain.model.Scenario

/**
 * Repositorio de escenarios.
 * Lee los escenarios desde los archivos .json de assets/content/scenarios/ (A1).
*/
interface ScenarioRepository {
suspend fun getAllScenarios(): List<Scenario>
suspend fun getScenarioById(id: String): Scenario?
}