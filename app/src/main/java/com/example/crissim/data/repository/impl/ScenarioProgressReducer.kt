package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.entity.ScenarioProgressEntity
import com.example.crissim.domain.model.AttemptResult
import com.example.crissim.domain.model.Scenario

/** Monotonic merge policy for one user's progress in one catalog scenario. */
object ScenarioProgressReducer {
    fun merge(
        current: ScenarioProgressEntity?,
        attempt: AttemptResult,
        scenario: Scenario
    ): ScenarioProgressEntity {
        val percentage = if (attempt.maxScore > 0) {
            (attempt.score.toLong() * 100 / attempt.maxScore).toInt().coerceIn(0, 100)
        } else 0
        val fullAttempt = attempt.decisions.size == scenario.nodes.count { it.options.isNotEmpty() }
        val passed = attempt.isPassed && fullAttempt
        val isBetter = current == null || percentage > current.bestPercentage ||
            (percentage == current.bestPercentage && attempt.score > current.bestScore) ||
            (percentage == current.bestPercentage && attempt.score == current.bestScore &&
                attempt.completedAt > (current.bestAttemptAt ?: Long.MIN_VALUE))

        return ScenarioProgressEntity(
            userId = attempt.userId,
            scenarioId = scenario.id,
            level = scenario.level.name,
            isRequiredForLevel = scenario.requiredForLevel,
            bestScore = if (isBetter) attempt.score else current.bestScore,
            maxScore = if (isBetter) attempt.maxScore else current.maxScore,
            bestPercentage = if (isBetter) percentage else current.bestPercentage,
            isCompleted = (current?.isCompleted == true) || passed,
            bestAttemptUuid = if (isBetter) attempt.attemptUuid else current.bestAttemptUuid,
            bestAttemptAt = if (isBetter) attempt.completedAt else current.bestAttemptAt,
            lastAttemptAt = maxOf(current?.lastAttemptAt ?: Long.MIN_VALUE, attempt.completedAt),
            scenarioVersion = scenario.version,
            updatedAt = maxOf(current?.updatedAt ?: 0L, attempt.completedAt)
        )
    }
}
