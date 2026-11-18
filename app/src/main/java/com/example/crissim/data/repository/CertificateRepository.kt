package com.example.crissim.data.repository

import com.example.crissim.domain.model.*

/**
 * Repositorio de certificados (RF-07, RF-09, A4).
 */
interface CertificateRepository {
    suspend fun generateCertificate(certificate: Certificate): Certificate
    suspend fun getUserCertificates(userId: Long): List<Certificate>
    suspend fun getCertificateById(id: Long): Certificate?
    suspend fun verifyCertificate(payload: CertificatePayload): VerificationResult
}
