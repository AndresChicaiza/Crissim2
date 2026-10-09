package com.example.crissim.data.local.dao

import androidx.room.*
import com.example.crissim.data.local.entity.UserEntity

/**
 * DAO para operaciones del perfil de usuario.
 */
@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getActiveUser(): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Delete
    suspend fun deleteUser(user: UserEntity)
}
