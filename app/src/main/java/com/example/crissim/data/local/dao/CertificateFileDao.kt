package com.example.crissim.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.crissim.data.local.entity.CertificateFileEntity

@Dao
interface CertificateFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: CertificateFileEntity)

    @Query("SELECT * FROM certificate_files WHERE certificateUuid = :certificateUuid")
    suspend fun get(certificateUuid: String): CertificateFileEntity?
}
