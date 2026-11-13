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

    @Query("SELECT * FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId ORDER BY revision DESC, savedAt DESC LIMIT 1")
    suspend fun getSavedState(userId: Long, scenarioId: String): SavedStateEntity?

    @Query("SELECT * FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId AND deviceId = :deviceId")
    suspend fun getSavedState(userId: Long, scenarioId: String, deviceId: String): SavedStateEntity?

    @Query("SELECT * FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId ORDER BY revision DESC, savedAt DESC")
    suspend fun getSavedStates(userId: Long, scenarioId: String): List<SavedStateEntity>

    @Query("DELETE FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId")
    suspend fun deleteSavedState(userId: Long, scenarioId: String)

    @Query("DELETE FROM saved_states WHERE userId = :userId AND scenarioId = :scenarioId AND deviceId = :deviceId")
    suspend fun deleteSavedState(userId: Long, scenarioId: String, deviceId: String)

    @Query("SELECT * FROM saved_states WHERE userId = :userId")
    suspend fun getAllSavedStates(userId: Long): List<SavedStateEntity>
}
