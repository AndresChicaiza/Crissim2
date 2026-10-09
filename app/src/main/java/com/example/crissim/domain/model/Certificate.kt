package com.example.crissim.domain.model

/**
 * Certificado digital generado al superar un nivel (RF-07).
 * A4: PDF con QR firmado (ECDSA). Verificable offline.
 */
data class Certificate(
    val id: Long = 0,
    val userId: Long,
    val userName: String,
    val level: Level,
    val scenarioId: String,
    val score: Int,
    val maxScore: Int,
    val issuedAt: Long = System.currentTimeMillis(),
    val signature: String = "",
    val pdfPath: String? = null
) {
    /**
     * Payload que se codifica en el QR.
     * Incluye todos los datos verificables del certificado.
     */
    fun buildQrPayload(): String {
        return buildString {
            append("CRISISSIM|")
            append("v1|")
            append("$id|")
            append("$userName|")
            append("${level.name}|")
            append("$scenarioId|")
            append("$score/$maxScore|")
            append("$issuedAt|")
            append(signature)
        }
    }

    companion object {
        /** Parsea un payload de QR y devuelve los datos, o null si es inválido. */
        fun parseQrPayload(payload: String): CertificatePayload? {
            return try {
                val parts = payload.split("|")
                if (parts.size < 9 || parts[0] != "CRISISSIM" || parts[1] != "v1") {
                    return null
                }
                val scoreParts = parts[6].split("/")
                CertificatePayload(
                    certificateId = parts[2].toLong(),
                    userName = parts[3],
                    level = Level.valueOf(parts[4]),
                    scenarioId = parts[5],
                    score = scoreParts[0].toInt(),
                    maxScore = scoreParts[1].toInt(),
                    issuedAt = parts[7].toLong(),
                    signature = parts[8]
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Datos extraídos de un QR de certificado.
 */
data class CertificatePayload(
    val certificateId: Long,
    val userName: String,
    val level: Level,
    val scenarioId: String,
    val score: Int,
    val maxScore: Int,
    val issuedAt: Long,
    val signature: String
)

/**
 * Resultado de la verificación de un certificado (RF-09).
 */
sealed class VerificationResult {
    data class Valid(val payload: CertificatePayload) : VerificationResult()
    data class Tampered(val reason: String) : VerificationResult()
    data object InvalidFormat : VerificationResult()
    data object NotFound : VerificationResult()
}

