package com.example.crissim.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** Durable operations for a future sync worker; not consumed by Firebase in this phase. */
@Entity(
    tableName = "sync_outbox",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId", "status", "createdAt"]), Index("aggregateId")]
)
data class SyncOutboxEntity(
    @PrimaryKey val operationUuid: String = UUID.randomUUID().toString(),
    val userId: Long,
    val aggregateType: String,
    val aggregateId: String,
    val operation: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "'PENDING'")
    val status: String = STATUS_PENDING,
    val attemptCount: Int = 0,
    val lastError: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_FAILED = "FAILED"
    }
}
