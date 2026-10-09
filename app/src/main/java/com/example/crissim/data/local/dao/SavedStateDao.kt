package com.example.crissim.data.local.dao

import androidx.room.*
import com.example.crissim.data.local.entity.SavedStateEntity

/**
 * DAO para operaciones de estado guardado del motor (RF-10, A3).
 */
@Dao
interface SavedStateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveState(state: SavedStateEntity)

    @Query("SELECT * FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId")
    suspend fun getSavedState(userId: Long, scenarioId: String): SavedStateEntity?

    @Query("DELETE FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId")
    suspend fun deleteSavedState(userId: Long, scenarioId: String)

    @Query("SELECT * FROM saved_states WHERE userId = :userId")
    suspend fun getAllSavedStates(userId: Long): List<SavedStateEntity>
}
