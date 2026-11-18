package com.example.crissim.data.repository.impl

import android.content.Context
import com.example.crissim.data.content.ScenarioParser
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.domain.model.Scenario

/**
 * Implementación del repositorio de escenarios.
 * Lee los JSON desde assets/content/scenarios/ (A1).
 * Los escenarios se cachean en memoria al primer acceso.
 */
class ScenarioRepositoryImpl(
    private val context: Context
) : ScenarioRepository {

    private var cachedScenarios: List<Scenario>? = null

    override suspend fun getAllScenarios(): List<Scenario> {
        return cachedScenarios ?: loadScenariosFromAssets().also {
            cachedScenarios = it
        }
    }

    override suspend fun getScenarioById(id: String): Scenario? {
        return getAllScenarios().find { it.id == id }
    }

    // ─── Funciones internas ───

    private fun loadScenariosFromAssets(): List<Scenario> {
        val scenarios = mutableListOf<Scenario>()
        val assetManager = context.assets

        try {
            val scenarioFiles = assetManager.list("content/scenarios") ?: emptyArray()

            for (fileName in scenarioFiles) {
                if (!fileName.endsWith(".json")) continue

                try {
                    val jsonString = assetManager
                        .open("content/scenarios/$fileName")
                        .bufferedReader()
                        .use { it.readText() }

                    val scenario = ScenarioParser.parseScenario(jsonString)
                    if (scenario != null) {
                        scenarios.add(scenario)
                    }
                } catch (e: Exception) {
                    // Log error pero no crash la app por un escenario corrupto
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return scenarios
    }
}
