package com.example.crissim.data.local.dao

import androidx.room.*
import com.example.crissim.data.local.entity.AttemptEntity

/**
 * DAO para operaciones de intentos de escenarios.
 */
@Dao
interface AttemptDao {

    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity): Long

    @Query("SELECT * FROM attempts WHERE userId = :userId AND scenarioId = :scenarioId ORDER BY completedAt DESC")
    suspend fun getAttemptsByScenario(userId: Long, scenarioId: String): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE userId = :userId ORDER BY completedAt DESC")
    suspend fun getAllAttempts(userId: Long): List<AttemptEntity>

    @Query("SELECT * FROM attempts WHERE userId = :userId AND scenarioId = :scenarioId ORDER BY score DESC LIMIT 1")
    suspend fun getBestAttempt(userId: Long, scenarioId: String): AttemptEntity?

    @Query("SELECT COUNT(*) FROM attempts WHERE userId = :userId AND scenarioId = :scenarioId")
    suspend fun getAttemptCount(userId: Long, scenarioId: String): Int
}
