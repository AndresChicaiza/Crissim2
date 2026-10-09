package com.example.crissim.data.repository.impl

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.example.crissim.data.local.dao.CertificateDao
import com.example.crissim.data.local.entity.CertificateEntity
import com.example.crissim.data.repository.CertificateRepository
import com.example.crissim.domain.model.Certificate
import com.example.crissim.domain.model.CertificatePayload
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.model.VerificationResult
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

/**
 * Implementación del repositorio de certificados (A4).
 * Firma con ECDSA (clave persistente en Android Keystore) y verifica offline.
 *
 * LIMITACIÓN CONOCIDA (ADR-001, A4): la clave es propia de cada dispositivo, así que un
 * certificado solo se verifica en el mismo dispositivo que lo emitió. Para verificarlo en
 * otro equipo (p. ej. el de la institución) hay que decidir una clave institucional.
 */
class CertificateRepositoryImpl(
    private val certificateDao: CertificateDao
) : CertificateRepository {

    private val keyPair: KeyPair by lazy { loadOrCreateKeyPair() }

    override suspend fun generateCertificate(certificate: Certificate): Certificate {
        // 1) Insertar sin firma para obtener el id definitivo.
        val id = certificateDao.insertCertificate(certificate.copy(signature = "").toEntity())

        // 2) Firmar con el id real: el verificador reconstruye exactamente este payload.
        val withId = certificate.copy(id = id, signature = "")
        val signed = withId.copy(signature = signData(withId.buildQrPayload()))

        // 3) Guardar la firma.
        certificateDao.updateCertificate(signed.toEntity())
        return signed
    }

    override suspend fun getUserCertificates(userId: Long): List<Certificate> {
        return certificateDao.getCertificatesByUser(userId).map { it.toDomain() }
    }

    override suspend fun getCertificateById(id: Long): Certificate? {
        return certificateDao.getCertificateById(id)?.toDomain()
    }

    override suspend fun verifyCertificate(payload: CertificatePayload): VerificationResult {
        val stored = certificateDao.getCertificateById(payload.certificateId)
            ?: return VerificationResult.NotFound

        if (stored.userName != payload.userName ||
            stored.level != payload.level.name ||
            stored.score != payload.score ||
            stored.maxScore != payload.maxScore
        ) {
            return VerificationResult.Tampered("Los datos del certificado no coinciden")
        }

        val dataToVerify = Certificate(
            id = payload.certificateId,
            userId = stored.userId,
            userName = payload.userName,
            level = payload.level,
            scenarioId = payload.scenarioId,
            score = payload.score,
            maxScore = payload.maxScore,
            issuedAt = payload.issuedAt,
            signature = ""
        ).buildQrPayload()

        return if (verifySignature(dataToVerify, payload.signature)) {
            VerificationResult.Valid(payload)
        } else {
            VerificationResult.Tampered("La firma digital no es válida")
        }
    }

    // ─── Criptografía ECDSA (Android Keystore) ───

    private fun loadOrCreateKeyPair(): KeyPair {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val privateKey = keyStore.getKey(KEY_ALIAS, null) as? PrivateKey
        val publicKey = keyStore.getCertificate(KEY_ALIAS)?.publicKey
        if (privateKey != null && publicKey != null) return KeyPair(publicKey, privateKey)

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        generator.initialize(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()
        )
        return generator.generateKeyPair()
    }

    private fun signData(data: String): String {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    private fun verifySignature(data: String, signatureStr: String): Boolean {
        return try {
            val signature = Signature.getInstance("SHA256withECDSA")
            signature.initVerify(keyPair.public)
            signature.update(data.toByteArray(Charsets.UTF_8))
            signature.verify(Base64.getDecoder().decode(signatureStr))
        } catch (e: Exception) {
            false
        }
    }

    // ─── Mappers ───

    private fun Certificate.toEntity(): CertificateEntity {
        return CertificateEntity(
            id = id,
            userId = userId,
            userName = userName,
            level = level.name,
            scenarioId = scenarioId,
            score = score,
            maxScore = maxScore,
            issuedAt = issuedAt,
            signature = signature,
            pdfPath = pdfPath
        )
    }

    private fun CertificateEntity.toDomain(): Certificate {
        return Certificate(
            id = id,
            userId = userId,
            userName = userName,
            level = Level.valueOf(level),
            scenarioId = scenarioId,
            score = score,
            maxScore = maxScore,
            issuedAt = issuedAt,
            signature = signature,
            pdfPath = pdfPath
        )
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "crisissim_certificate_key"
    }
}
