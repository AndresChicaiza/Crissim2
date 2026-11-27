package com.example.crissim.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.crissim.domain.model.Badge
import com.example.crissim.domain.model.MicroTraining
import com.example.crissim.domain.model.Streak
import com.example.crissim.domain.model.UserBadge
import com.example.crissim.domain.model.UserMicroTraining

@Dao
interface MicroTrainingDao {

    // --- MicroTraining ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMicroTraining(microTraining: MicroTraining): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMicroTrainings(microTrainings: List<MicroTraining>)

    @Query("SELECT * FROM micro_training WHERE id = :id")
    suspend fun getMicroTrainingById(id: Long): MicroTraining?

    @Query("SELECT * FROM micro_training WHERE protocolId = :protocolId AND isActive = 1 ORDER BY \"order\" ASC")
    suspend fun getMicroTrainingsByProtocol(protocolId: String): List<MicroTraining>

    @Query("SELECT * FROM micro_training WHERE isActive = 1 ORDER BY \"order\" ASC")
    suspend fun getAllActiveMicroTrainings(): List<MicroTraining>

    @Query("SELECT * FROM micro_training WHERE difficulty = :difficulty AND isActive = 1 ORDER BY \"order\" ASC")
    suspend fun getMicroTrainingsByDifficulty(difficulty: String): List<MicroTraining>

    @Query("SELECT * FROM micro_training WHERE EXISTS (SELECT 1 FROM json_each(tags) WHERE value = :tag) AND isActive = 1 ORDER BY \"order\" ASC")
    suspend fun getMicroTrainingsByTag(tag: String): List<MicroTraining>

    @Update
    suspend fun updateMicroTraining(microTraining: MicroTraining)

    @Delete
    suspend fun deleteMicroTraining(microTraining: MicroTraining)

    // --- Badge ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)

    @Query("SELECT * FROM badge WHERE id = :id")
    suspend fun getBadgeById(id: String): Badge?

    @Query("SELECT * FROM badge WHERE category = :category")
    suspend fun getBadgesByCategory(category: String): List<Badge>

    @Query("SELECT * FROM badge ORDER BY \"order\" ASC")
    suspend fun getAllBadges(): List<Badge>

    @Update
    suspend fun updateBadge(badge: Badge)

    @Query("SELECT * FROM badge WHERE id IN (SELECT badgeId FROM user_badge WHERE userId = :userId) ORDER BY earnedAt DESC")
    suspend fun getUserEarnedBadges(userId: Long): List<Badge>

    @Query("SELECT * FROM badge WHERE id NOT IN (SELECT badgeId FROM user_badge WHERE userId = :userId) ORDER BY \"order\" ASC")
    suspend fun getUnearnedBadges(userId: Long): List<Badge>

    @Query("SELECT * FROM badge WHERE earnedAt > 0 ORDER BY earnedAt DESC")
    suspend fun getEarnedBadges(): List<Badge>

    // --- Streak ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStreak(streak: Streak)

    @Query("SELECT * FROM streak WHERE userId = :userId")
    suspend fun getStreak(userId: Long): Streak?

    @Update
    suspend fun updateStreak(streak: Streak)

    // --- UserMicroTraining ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMicroTraining(userMicroTraining: UserMicroTraining): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMicroTrainings(userMicroTrainings: List<UserMicroTraining>)

    @Query("SELECT * FROM user_micro_training WHERE userId = :userId AND microTrainingId = :microTrainingId")
    suspend fun getUserMicroTraining(userId: Long, microTrainingId: Long): UserMicroTraining?

    @Query("SELECT * FROM user_micro_training WHERE userId = :userId")
    suspend fun getUserMicroTrainings(userId: Long): List<UserMicroTraining>

    @Query("SELECT * FROM user_micro_training WHERE userId = :userId AND status = :status")
    suspend fun getUserMicroTrainingsByStatus(userId: Long, status: String): List<UserMicroTraining>

    @Update
    suspend fun updateUserMicroTraining(userMicroTraining: UserMicroTraining)

    // --- UserBadge ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserBadge(userBadge: UserBadge)

    @Query("SELECT * FROM user_badge WHERE userId = :userId AND badgeId = :badgeId")
    suspend fun getUserBadge(userId: Long, badgeId: String): UserBadge?

    @Query("SELECT * FROM user_badge WHERE userId = :userId")
    suspend fun getUserBadges(userId: Long): List<UserBadge>
}
