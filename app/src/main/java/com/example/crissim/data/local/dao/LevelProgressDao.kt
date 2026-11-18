package com.example.crissim.data.local.dao

import androidx.room.*
import com.example.crissim.data.local.entity.LevelProgressEntity

/**
 * DAO para operaciones de progreso por nivel.
 */
@Dao
interface LevelProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: LevelProgressEntity)

    @Query("SELECT * FROM level_progress WHERE userId = :userId AND level = :level")
    suspend fun getProgress(userId: Long, level: String): LevelProgressEntity?

    @Query("SELECT * FROM level_progress WHERE userId = :userId")
    suspend fun getAllProgress(userId: Long): List<LevelProgressEntity>

    @Query("UPDATE level_progress SET bestScore = :score, isCompleted = :completed, attempts = attempts + 1, lastAttemptAt = :timestamp WHERE userId = :userId AND level = :level")
    suspend fun updateProgress(userId: Long, level: String, score: Int, completed: Boolean, timestamp: Long)
}
