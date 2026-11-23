package com.example.crissim.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.crissim.data.certificate.CertificatePdfGenerator
import com.example.crissim.domain.model.*
import com.example.crissim.data.repository.CertificateRepository
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.data.repository.UserRepository

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

/**
 * Caso de uso: generar PDF del certificado y guardar la ruta en BD.
 * Se llama desde la UI cuando el usuario pide vista previa o compartir.
 */
class GenerateCertificatePdfUseCase(
    private val certificateRepository: CertificateRepository
) {
    suspend fun execute(context: Context, certificateId: Long): Uri? {
        val certificate = certificateRepository.getCertificateById(certificateId) ?: return null
        val pdfUri = CertificatePdfGenerator.generateCertificatePdf(context, certificate)
        pdfUri?.let { uri ->
            certificateRepository.updateCertificatePdfPath(certificateId, uri.toString())
        }
        return pdfUri
    }
}

/**
 * Caso de uso: generar certificados para niveles completados que aún no tengan certificado.
 * Se llama tras actualizar el progreso (p. ej. en LevelsViewModel.refresh()).
 */
class GenerateCertificatesForCompletedLevelsUseCase(
    private val certificateRepository: CertificateRepository,
    private val progressRepository: ProgressRepository,
    private val scenarioRepository: ScenarioRepository,
    private val userRepository: UserRepository
) {
    suspend fun execute(userId: Long) {
        val user = userRepository.getUserById(userId) ?: return
        val userName = user.name

        // Obtener certificados existentes para este usuario
        val existingCerts = certificateRepository.getUserCertificates(userId)
            .associateBy { it.level }

        // Obtener progreso de todos los niveles
        val allProgress = progressRepository.getAllLevelProgress(userId)

        for (levelProgress in allProgress) {
            // Solo generar si el nivel está completado y no tiene certificado
            if (levelProgress.isCompleted && !existingCerts.containsKey(levelProgress.level)) {
                // Buscar el mejor escenario aprobado de este nivel para usar como scenarioId
                val scenarios = scenarioRepository.getAllScenarios()
                    .filter { it.level == levelProgress.level && it.requiredForLevel }

                if (scenarios.isNotEmpty()) {
                    // Usar el escenario con mejor puntuación
                    val bestScenario = scenarios.maxByOrNull { it.calculateMaxScore() } ?: scenarios.first()
                    val maxScore = bestScenario.calculateMaxScore()

                    // Generar certificado
                    val certificate = Certificate(
                        userId = userId,
                        userName = userName,
                        level = levelProgress.level,
                        scenarioId = bestScenario.id,
                        score = maxScore,
                        maxScore = maxScore
                    )
                    certificateRepository.generateCertificate(certificate)
                }
            }
        }
    }
}
