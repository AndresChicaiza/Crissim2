package com.example.crissim.domain.engine

import com.example.crissim.data.content.ScenarioParser
import com.example.crissim.domain.model.AttemptResult
import com.example.crissim.domain.model.EndType
import com.example.crissim.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** JVM tests for the engine using the current fire scenario catalog. */
class ScenarioEngineTest {
    private lateinit var scenario: Scenario

    @Before
    fun setUp() {
        val file = File("src/main/assets/content/scenarios/fire-basic-01.json")
        scenario = requireNotNull(ScenarioParser.parseScenario(file.readText())) { "Invalid scenario JSON" }
    }

    @Test
    fun puntajeMaximoEsLaSumaDelCaminoOptimoDelJson() {
        assertEquals(50, scenario.calculateMaxScore())
    }

    @Test
    fun caminoOptimoAlcanzaElPuntajeMaximoYAprobacion() {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        optimalPath().forEach { optionId ->
            assertNotNull("Invalid option $optionId", engine.selectOption(optionId))
        }
        val result = requireNotNull(engine.getAttemptResult(userId = 1))
        assertEquals(scenario.calculateMaxScore(), result.score)
        assertEquals(scenario.nodes.count { it.options.isNotEmpty() }, result.decisions.size)
        assertEquals(EndType.SUCCESS, result.endType)
        assertTrue(result.isPassed)
    }

    @Test
    fun esperarTodosLosTiemposNoApruebaNiSumaPuntos() {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        var guard = 0
        while (!engine.state.isFinished && guard++ < 20) {
            assertNotNull(engine.handleTimeout())
        }
        assertTrue(engine.state.isFinished)
        val result = requireNotNull(engine.getAttemptResult(userId = 1))
        assertFalse(result.isPassed)
        assertEquals(0, result.score)
    }

    @Test
    fun porcentajeDeAprobacionRespetaElUmbralDelJson() {
        val exactlyAtThreshold = playWithCorrectAnswers(4)
        assertEquals(80, exactlyAtThreshold.score * 100 / exactlyAtThreshold.maxScore)
        assertTrue(exactlyAtThreshold.isPassed)

        val belowThreshold = playWithCorrectAnswers(3)
        assertEquals(60, belowThreshold.score * 100 / belowThreshold.maxScore)
        assertFalse(belowThreshold.isPassed)
    }

    @Test
    fun opcionInexistenteNoCambiaElEstado() {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        assertEquals(null, engine.selectOption("does-not-exist"))
        assertEquals(0, engine.state.score)
    }

    /** Finds a maximum scoring route by traversing the choices in the scenario graph. */
    private fun optimalPath(): List<String> {
        fun bestFrom(nodeId: String): Pair<Int, List<String>> {
            val node = requireNotNull(scenario.getNode(nodeId))
            if (node.isEnd || node.options.isEmpty()) return 0 to emptyList()
            return node.options.map { option ->
                val tail = bestFrom(option.nextNodeId)
                (option.scoreChange + tail.first) to (listOf(option.id) + tail.second)
            }.maxBy { it.first }
        }
        return bestFrom(scenario.startNodeId).second
    }

    private fun playWithCorrectAnswers(correctAnswers: Int): AttemptResult {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        var correctChosen = 0
        while (!engine.state.isFinished) {
            val node = requireNotNull(engine.state.currentNode)
            val isCorrectChoice = correctChosen < correctAnswers
            val option = if (isCorrectChoice) {
                node.options.first { it.scoreChange > 0 }
            } else {
                node.options.first { it.scoreChange == 0 }
            }
            assertNotNull(engine.selectOption(option.id))
            if (isCorrectChoice) correctChosen++
        }
        return requireNotNull(engine.getAttemptResult(userId = 1))
    }
}
