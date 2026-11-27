package com.example.crissim.domain.usecase

import com.example.crissim.data.repository.CertificateRepository
import com.example.crissim.domain.model.Certificate
import com.example.crissim.domain.model.CertificatePayload
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.model.VerificationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class VerifyCertificateUseCaseTest {

    private lateinit var fakeRepository: FakeCertificateRepository
    private lateinit var useCase: VerifyCertificateUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeCertificateRepository()
        useCase = VerifyCertificateUseCase(fakeRepository)

        // Seed a valid certificate in repository
        val cert = Certificate(
            id = 100L,
            userId = 1L,
            userName = "Valentina Rojas",
            level = Level.BASIC,
            scenarioId = "sis-01",
            score = 100,
            maxScore = 100,
            issuedAt = 1700000000000L,
            signature = "valid_signature_123"
        )
        fakeRepository.certs[100L] = cert
    }

    @Test
    fun verifyValidQr_returnsValidResult() = runBlocking {
        val validQr = "CRISISSIM|v1|100|Valentina Rojas|BASIC|sis-01|100/100|1700000000000|valid_signature_123"

        val result = useCase.execute(validQr)

        assertTrue(result is VerificationResult.Valid)
        val valid = result as VerificationResult.Valid
        assertEquals("Valentina Rojas", valid.payload.userName)
        assertEquals(Level.BASIC, valid.payload.level)
    }

    @Test
    fun verifyTamperedQr_returnsTamperedResult() = runBlocking {
        // Tampered score 100 -> 500
        val tamperedQr = "CRISISSIM|v1|100|Valentina Rojas|BASIC|sis-01|500/100|1700000000000|valid_signature_123"

        val result = useCase.execute(tamperedQr)

        assertTrue(result is VerificationResult.Tampered)
    }

    @Test
    fun verifyForeignAppQr_returnsInvalidFormatOrNotFound() = runBlocking {
        val foreignQr = "OTHERAPP|v1|999|John Doe|ADVANCED|100/100"

        val result = useCase.execute(foreignQr)

        assertTrue(result is VerificationResult.InvalidFormat)
    }

    @Test
    fun verifyNonExistingCertificate_returnsNotFound() = runBlocking {
        val unknownCertQr = "CRISISSIM|v1|9999|Juan Perez|BASIC|sis-01|100/100|1700000000000|signature_999"

        val result = useCase.execute(unknownCertQr)

        assertTrue(result is VerificationResult.NotFound)
    }
}

class FakeCertificateRepository : CertificateRepository {
    val certs = mutableMapOf<Long, Certificate>()

    override suspend fun generateCertificate(certificate: Certificate): Certificate {
        certs[certificate.id] = certificate
        return certificate
    }

    override suspend fun getUserCertificates(userId: Long): List<Certificate> {
        return certs.values.filter { it.userId == userId }
    }

    override suspend fun getCertificateById(id: Long): Certificate? = certs[id]

    override suspend fun verifyCertificate(payload: CertificatePayload): VerificationResult {
        val stored = certs[payload.certificateId] ?: return VerificationResult.NotFound

        if (stored.userName != payload.userName ||
            stored.level != payload.level ||
            stored.score != payload.score ||
            stored.maxScore != payload.maxScore
        ) {
            return VerificationResult.Tampered("Los datos del certificado no coinciden")
        }

        if (payload.signature != stored.signature) {
            return VerificationResult.Tampered("La firma digital no es válida")
        }

        return VerificationResult.Valid(payload)
    }

    override suspend fun updateCertificatePdfPath(certificateId: Long, pdfPath: String) {
        val existing = certs[certificateId]
        if (existing != null) {
            certs[certificateId] = existing.copy(pdfPath = pdfPath)
        }
    }
}
