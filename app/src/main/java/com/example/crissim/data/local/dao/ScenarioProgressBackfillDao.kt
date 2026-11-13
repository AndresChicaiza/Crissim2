package com.example.crissim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.crissim.data.local.entity.ScenarioProgressBackfillEntity

@Dao
interface ScenarioProgressBackfillDao {
    @Query("SELECT EXISTS(SELECT 1 FROM scenario_progress_backfill WHERE userId = :userId)")
    suspend fun isComplete(userId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markComplete(marker: ScenarioProgressBackfillEntity)
}
