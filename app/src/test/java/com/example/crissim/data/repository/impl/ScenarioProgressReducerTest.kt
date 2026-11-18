package com.example.crissim.data.repository.impl

import com.example.crissim.data.content.ScenarioParser
import com.example.crissim.data.local.entity.ScenarioProgressEntity
import com.example.crissim.domain.model.AttemptResult
import com.example.crissim.domain.model.DecisionRecord
import com.example.crissim.domain.model.EndType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ScenarioProgressReducerTest {
    private val scenario = requireNotNull(
        ScenarioParser.parseScenario(File("src/main/assets/content/scenarios/fire-basic-01.json").readText())
    )

    private fun attempt(score: Int, passed: Boolean, at: Long) = AttemptResult(
        scenarioId = scenario.id,
        userId = 10,
        score = score,
        maxScore = scenario.calculateMaxScore(),
        isPassed = passed,
        decisions = List(scenario.nodes.count { it.options.isNotEmpty() }) {
            DecisionRecord("node$it", "option$it", 0, false, 1)
        },
        startedAt = at - 100,
        completedAt = at,
        endNodeId = "end",
        endType = EndType.SUCCESS
    )

    @Test
    fun progresoConservaMejorPuntajeYFinalizacion() {
        val passed = ScenarioProgressReducer.merge(null, attempt(30, true, 100), scenario)
        val lower = ScenarioProgressReducer.merge(passed, attempt(10, false, 200), scenario)

        assertEquals(30, lower.bestScore)
        assertEquals(100L, lower.bestAttemptAt)
        assertEquals(200L, lower.lastAttemptAt)
        assertTrue(lower.isCompleted)
    }

    @Test
    fun resultadoAprobadoIncompletoNoCompletaEscenario() {
        val incomplete = attempt(40, true, 100).copy(decisions = emptyList())
        val progress = ScenarioProgressReducer.merge(null, incomplete, scenario)
        assertEquals(false, progress.isCompleted)
    }

    @Test
    fun nivelCompletaSoloCuandoTodosLosEscenariosRequeridosEstanCompletos() {
        val catalog = File("src/main/assets/content/scenarios").listFiles()
            .orEmpty().filter { it.extension == "json" }
            .mapNotNull { ScenarioParser.parseScenario(it.readText()) }
            .filter { it.level == scenario.level }
        val required = catalog.filter { it.requiredForLevel }
        val progress = required.dropLast(1).associate { item ->
            item.id to ScenarioProgressEntity(1, item.id, item.level.name, isCompleted = true)
        }
        assertEquals(false, ScenarioProgressPolicy.isLevelComplete(catalog, progress))
        val completed = required.associate { item ->
            item.id to ScenarioProgressEntity(1, item.id, item.level.name, isCompleted = true)
        }
        assertEquals(required.isNotEmpty(), ScenarioProgressPolicy.isLevelComplete(catalog, completed))
    }
}
