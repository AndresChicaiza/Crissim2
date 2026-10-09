package com.example.crissim.data.local.dao

import androidx.room.*
import com.example.crissim.data.local.entity.CertificateEntity

/**
 * DAO para operaciones de certificados.
 */
@Dao
interface CertificateDao {

    @Insert
    suspend fun insertCertificate(certificate: CertificateEntity): Long

    @Update
    suspend fun updateCertificate(certificate: CertificateEntity)

    @Query("SELECT * FROM certificates WHERE userId = :userId ORDER BY issuedAt DESC")
    suspend fun getCertificatesByUser(userId: Long): List<CertificateEntity>

    @Query("SELECT * FROM certificates WHERE id = :certificateId")
    suspend fun getCertificateById(certificateId: Long): CertificateEntity?

    @Query("SELECT * FROM certificates WHERE userId = :userId AND level = :level AND scenarioId = :scenarioId LIMIT 1")
    suspend fun getCertificate(userId: Long, level: String, scenarioId: String): CertificateEntity?
}
