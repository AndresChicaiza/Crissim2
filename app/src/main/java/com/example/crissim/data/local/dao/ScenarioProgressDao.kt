package com.example.crissim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.crissim.data.local.entity.ScenarioProgressEntity

@Dao
interface ScenarioProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(progress: ScenarioProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(progress: List<ScenarioProgressEntity>)

    @Query("SELECT * FROM scenario_progress WHERE userId = :userId AND scenarioId = :scenarioId")
    suspend fun get(userId: Long, scenarioId: String): ScenarioProgressEntity?

    @Query("SELECT * FROM scenario_progress WHERE userId = :userId")
    suspend fun getAll(userId: Long): List<ScenarioProgressEntity>

    @Query("SELECT * FROM scenario_progress WHERE userId = :userId AND level = :level")
    suspend fun getByLevel(userId: Long, level: String): List<ScenarioProgressEntity>
}
