package com.example.crissim.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.crissim.data.local.dao.CertificateDao
import com.example.crissim.data.local.dao.CertificateFileDao
import com.example.crissim.domain.model.CertificatePayload
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.model.VerificationResult
import java.io.InputStream
import java.security.MessageDigest

/**
 * Caso de uso: verificar la autenticidad e integridad de un archivo PDF de certificado (HU-19 / CA-4).
 * Calcula el hash SHA-256 del PDF y lo valida contra el registro firmado local.
 */
class VerifyPdfFileUseCase(
    private val certificateDao: CertificateDao,
    private val certificateFileDao: CertificateFileDao
) {
    suspend fun execute(context: Context, pdfUri: Uri): VerificationResult {
        return try {
            val inputStream = context.contentResolver.openInputStream(pdfUri)
                ?: return VerificationResult.Tampered("No se pudo abrir el archivo PDF")

            val computedHash = computeSha256(inputStream)
            if (computedHash.isEmpty()) return VerificationResult.Tampered("Error al calcular la huella digital del PDF")

            // Buscar si existe un certificado registrado con ese SHA-256
            val allCertEntities = certificateDao.getAllCertificates()
            for (certEntity in allCertEntities) {
                val fileEntity = certificateFileDao.get(certEntity.certificateUuid)
                if (fileEntity != null && fileEntity.sha256 == computedHash) {
                    val payload = CertificatePayload(
                        certificateId = certEntity.id,
                        userName = certEntity.userName,
                        level = Level.valueOf(certEntity.level),
                        scenarioId = certEntity.scenarioId,
                        score = certEntity.score,
                        maxScore = certEntity.maxScore,
                        issuedAt = certEntity.issuedAt,
                        signature = certEntity.signature
                    )
                    return VerificationResult.Valid(payload)
                }
            }

            // Si ningún SHA-256 coincide pero la URI estaba asociada a un certificado en el dispositivo:
            val matchedUriCert = allCertEntities.firstOrNull { certEntity ->
                val fileEntity = certificateFileDao.get(certEntity.certificateUuid)
                fileEntity?.localUri == pdfUri.toString()
            }

            if (matchedUriCert != null) {
                VerificationResult.Tampered("El contenido del archivo PDF fue alterado después de su emisión")
            } else {
                VerificationResult.NotFound
            }
        } catch (e: Exception) {
            VerificationResult.Tampered("Archivo no válido o modificado: ${e.message}")
        }
    }

    companion object {
        fun computeSha256(inputStream: InputStream): String {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                inputStream.use { stream ->
                    while (stream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                ""
            }
        }
    }
}
