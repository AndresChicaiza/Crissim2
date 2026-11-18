package com.example.crissim.domain.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash con sal del PIN local (HU-04 CA-3). El PIN nunca se guarda en texto plano.
 *
 * Formato de [hash]: `pbkdf2_sha256$<iteraciones>$<hash en Base64>`.
 * Las iteraciones viajan en el propio valor, así se pueden subir más adelante
 * sin invalidar los PIN ya guardados. La sal se guarda aparte, en Base64.
 */
object PinHasher {
    const val ITERATIONS = 120_000
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2_sha256"
    private const val SEPARATOR = "\$"
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    private val random = SecureRandom()

    /** Sal aleatoria de 16 bytes, codificada en Base64. */
    fun newSalt(): String {
        val salt = ByteArray(SALT_BYTES)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
    }

    fun hash(pin: String, saltBase64: String, iterations: Int = ITERATIONS): String {
        val salt = Base64.getDecoder().decode(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
            return listOf(PREFIX, iterations.toString(), Base64.getEncoder().encodeToString(bytes))
                .joinToString(SEPARATOR)
        } finally {
            spec.clearPassword()
        }
    }

    /** Comparación en tiempo constante. Devuelve false si el valor guardado es inválido. */
    fun verify(pin: String, saltBase64: String, stored: String): Boolean {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 3 || parts[0] != PREFIX) return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val expected = runCatching { hash(pin, saltBase64, iterations) }.getOrNull() ?: return false
        return MessageDigest.isEqual(expected.toByteArray(), stored.toByteArray())
    }
}