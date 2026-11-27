package com.example.crissim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.crissim.data.local.entity.LiveDrillEntity

@Dao
interface LiveDrillDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrill(drill: LiveDrillEntity)

    @Query("SELECT * FROM live_drill WHERE id = :id")
    suspend fun getDrillById(id: String): LiveDrillEntity?

    @Query("SELECT * FROM live_drill WHERE userId = :userId ORDER BY startedAt DESC")
    suspend fun getDrillsForUser(userId: Long): List<LiveDrillEntity>
}
