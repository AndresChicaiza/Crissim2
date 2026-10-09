package com.example.crissim.domain.engine

import com.example.crissim.data.content.ScenarioParser
import com.example.crissim.domain.model.EndType
import com.example.crissim.domain.model.Scenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/** Pruebas JVM del motor (sin Android). El directorio de trabajo de los tests es app/. */
class ScenarioEngineTest {

    private lateinit var scenario: Scenario

    @Before
    fun setUp() {
        val file = File("src/main/assets/content/scenarios/fire-basic-01.json")
        scenario = requireNotNull(ScenarioParser.parseScenario(file.readText())) { "JSON inválido" }
    }

    @Test
    fun puntajeMaximo_esElCaminoOptimo() {
        assertEquals(40, scenario.calculateMaxScore())
    }

    @Test
    fun caminoOptimo_terminaEnExitoYAprueba() {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        listOf("opt_01a", "opt_02a", "opt_03a", "opt_04a").forEach {
            assertNotNull("Opción $it no válida", engine.selectOption(it))
        }
        val result = requireNotNull(engine.getAttemptResult(userId = 1))
        assertEquals(40, result.score)
        assertEquals(EndType.SUCCESS, result.endType)
        assertTrue(result.isPassed)
    }

    @Test
    fun esperarTodosLosTiempos_nuncaApruebaNiPremia() {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        var guard = 0
        while (!engine.state.isFinished && guard++ < 20) {
            assertNotNull(engine.handleTimeout())
        }
        assertTrue(engine.state.isFinished)
        val result = requireNotNull(engine.getAttemptResult(userId = 1))
        assertFalse(result.isPassed)
        assertTrue(result.score < 0)
    }

    @Test
    fun opcionInexistente_noCambiaElEstado() {
        val engine = ScenarioEngine()
        engine.startScenario(scenario)
        assertEquals(null, engine.selectOption("no-existe"))
        assertEquals(0, engine.state.score)
    }
}
