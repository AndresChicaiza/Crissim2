package com.example.crissim.data.content

import com.example.crissim.domain.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Parser de escenarios JSON → modelo de dominio (A1).
 *
 * Formato del JSON esperado:
 * {
 *   "id": "fire-basic-01",
 *   "version": "1.0",
 *   "type": "fire",
 *   "level": "basic",
 *   "title": { "es": "Incendio en el aula", "en": "Classroom Fire" },
 *   "description": { "es": "...", "en": "..." },
 *   "start_node": "node_01",
 *   "passing_score": 70,
 *   "max_attempts": 3,
 *   "nodes": [ ... ]
 * }
 */
object ScenarioParser {

    private val gson = Gson()

    /**
     * Parsea un JSON string a un objeto Scenario.
     * Retorna null si el JSON es inválido.
     */
    fun parseScenario(jsonString: String): Scenario? {
        return try {
            val root = JsonParser.parseString(jsonString).asJsonObject
            parseScenarioFromJson(root)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseScenarioFromJson(json: JsonObject): Scenario {
        val nodesArray = json.getAsJsonArray("nodes")
        val nodes = nodesArray.map { parseNode(it.asJsonObject) }

        return Scenario(
            id = json.get("id").asString,
            version = json.get("version")?.asString ?: "1.0",
            type = parseScenarioType(json.get("type").asString),
            level = parseLevel(json.get("level").asString),
            title = parseLocalizedText(json.getAsJsonObject("title")),
            description = parseLocalizedText(json.getAsJsonObject("description")),
            startNodeId = json.get("start_node").asString,
            passingScore = json.get("passing_score")?.asInt ?: 70,
            maxAttempts = json.get("max_attempts")?.asInt ?: 3,
            nodes = nodes
        )
    }

    private fun parseNode(json: JsonObject): ScenarioNode {
        val options = json.getAsJsonArray("options")?.map {
            parseOption(it.asJsonObject)
        } ?: emptyList()

        return ScenarioNode(
            id = json.get("id").asString,
            narrative = parseLocalizedText(json.getAsJsonObject("narrative")),
            imageRef = json.get("image_ref")?.asString,
            timeLimitSeconds = json.get("time_limit_seconds")?.asInt,
            defaultOptionIndex = json.get("default_option_index")?.asInt,
            options = options,
            isEnd = json.get("is_end")?.asBoolean ?: false,
            endType = json.get("end_type")?.asString?.let { parseEndType(it) },
            feedback = json.getAsJsonObject("feedback")?.let { parseFeedback(it) }
        )
    }

    private fun parseOption(json: JsonObject): NodeOption {
        return NodeOption(
            id = json.get("id").asString,
            text = parseLocalizedText(json.getAsJsonObject("text")),
            nextNodeId = json.get("next_node").asString,
            scoreChange = json.get("score_change")?.asInt ?: 0,
            feedback = json.getAsJsonObject("feedback")?.let { parseFeedback(it) }
        )
    }

    private fun parseFeedback(json: JsonObject): Feedback {
        return Feedback(
            text = parseLocalizedText(json.getAsJsonObject("text")),
            isCorrect = json.get("is_correct")?.asBoolean ?: false,
            protocolSource = json.get("protocol_source")?.asString ?: "",
            protocolRef = json.get("protocol_ref")?.asString
        )
    }

    private fun parseLocalizedText(json: JsonObject?): LocalizedText {
        if (json == null) return LocalizedText(es = "")
        return LocalizedText(
            es = json.get("es")?.asString ?: "",
            en = json.get("en")?.asString
        )
    }

    private fun parseScenarioType(type: String): ScenarioType {
        return when (type.lowercase()) {
            "earthquake", "sismo" -> ScenarioType.EARTHQUAKE
            "fire", "incendio" -> ScenarioType.FIRE
            else -> ScenarioType.FIRE
        }
    }

    private fun parseLevel(level: String): Level {
        return when (level.lowercase()) {
            "basic", "basico", "básico" -> Level.BASIC
            "intermediate", "intermedio" -> Level.INTERMEDIATE
            "advanced", "avanzado" -> Level.ADVANCED
            else -> Level.BASIC
        }
    }

    private fun parseEndType(type: String): EndType {
        return when (type.lowercase()) {
            "success", "exito", "éxito" -> EndType.SUCCESS
            "partial_success", "parcial" -> EndType.PARTIAL_SUCCESS
            "failure", "fallo" -> EndType.FAILURE
            else -> EndType.SUCCESS
        }
    }
}
