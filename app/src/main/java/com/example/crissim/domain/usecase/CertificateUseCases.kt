package com.example.crissim.domain.usecase

import com.example.crissim.domain.model.*
import com.example.crissim.data.repository.CertificateRepository

/**
 * Caso de uso: generar certificado al superar un nivel (RF-07).
 */
class GenerateCertificateUseCase(
    private val certificateRepository: CertificateRepository
) {
    suspend fun execute(
        userId: Long,
        userName: String,
        level: Level,
        scenarioId: String,
        score: Int,
        maxScore: Int
    ): Certificate {
        val certificate = Certificate(
            userId = userId,
            userName = userName,
            level = level,
            scenarioId = scenarioId,
            score = score,
            maxScore = maxScore
        )
        return certificateRepository.generateCertificate(certificate)
    }
}

/**
 * Caso de uso: obtener todos los certificados de un usuario.
 */
class GetUserCertificatesUseCase(
    private val certificateRepository: CertificateRepository
) {
    suspend fun execute(userId: Long): List<Certificate> {
        return certificateRepository.getUserCertificates(userId)
    }
}

/**
 * Caso de uso: verificar un certificado a partir de su QR (RF-09).
 */
class VerifyCertificateUseCase(
    private val certificateRepository: CertificateRepository
) {
    suspend fun execute(qrPayload: String): VerificationResult {
        val payload = Certificate.parseQrPayload(qrPayload)
            ?: return VerificationResult.InvalidFormat

        return certificateRepository.verifyCertificate(payload)
    }
}
