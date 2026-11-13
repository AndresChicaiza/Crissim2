package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.entity.ScenarioProgressEntity
import com.example.crissim.domain.model.Scenario

/** Completion requires a non-empty catalog set and completion of every required scenario. */
object ScenarioProgressPolicy {
    fun isLevelComplete(
        scenarios: List<Scenario>,
        progressByScenarioId: Map<String, ScenarioProgressEntity>
    ): Boolean {
        val required = scenarios.filter { it.requiredForLevel }
        return required.isNotEmpty() && required.all { progressByScenarioId[it.id]?.isCompleted == true }
    }
}
