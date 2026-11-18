package com.example.crissim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.crissim.data.local.entity.SyncOutboxEntity

@Dao
interface SyncOutboxDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(operation: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox WHERE userId = :userId AND status = 'PENDING' ORDER BY createdAt, operationUuid")
    suspend fun getPending(userId: Long): List<SyncOutboxEntity>

    @Query("UPDATE sync_outbox SET attemptCount = attemptCount + 1, lastError = :error, status = 'FAILED' WHERE operationUuid = :operationUuid")
    suspend fun markFailed(operationUuid: String, error: String)

    @Query("UPDATE sync_outbox SET status = 'PENDING' WHERE operationUuid = :operationUuid")
    suspend fun retry(operationUuid: String)

    @Query("DELETE FROM sync_outbox WHERE operationUuid = :operationUuid")
    suspend fun markSynced(operationUuid: String)
}
