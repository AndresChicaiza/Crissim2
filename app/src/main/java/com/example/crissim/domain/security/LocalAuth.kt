package com.example.crissim.domain.security

import com.example.crissim.domain.model.UserProfile

/** Error de validación por campo (HU-03 CA-2). La interfaz lo traduce a texto. */
enum class FieldError {
    NAME_REQUIRED,
    CODE_REQUIRED,
    CODE_TOO_SHORT,
    CODE_INVALID_CHARS,
    PIN_INVALID
}

data class FieldErrors(
    val name: FieldError? = null,
    val code: FieldError? = null,
    val pin: FieldError? = null
) {
    val hasErrors: Boolean get() = name != null || code != null || pin != null
}

/** Resultado del registro y del acceso locales (HU-03 / HU-04). Ninguno usa red. */
sealed interface LocalAuthResult {
    data class Success(val user: UserProfile) : LocalAuthResult
    data class InvalidFields(val errors: FieldErrors) : LocalAuthResult
    /** El código ya existe en el dispositivo (HU-03 CA-3): se indica ingresar con su PIN. */
    data object CodeAlreadyRegistered : LocalAuthResult
    /** Ya hay otro perfil en el dispositivo (un perfil por dispositivo). */
    data object DeviceHasOtherProfile : LocalAuthResult
    /** Código o PIN incorrectos. [attemptsLeft] es null cuando el código no existe. */
    data class InvalidCredentials(val attemptsLeft: Int?) : LocalAuthResult
    /** Bloqueo temporal por intentos fallidos (HU-04 CA-2). */
    data class Locked(val remainingMillis: Long) : LocalAuthResult
}

/** Reglas de validación y bloqueo, sin dependencias de Android para probarlas en JVM. */
object LocalAuthPolicy {
    const val MAX_FAILED_ATTEMPTS = 5
    const val LOCK_MILLIS = 60_000L
    const val MIN_CODE_LENGTH = 4
    const val MAX_CODE_LENGTH = 20
    const val MIN_PIN_LENGTH = 4
    const val MAX_PIN_LENGTH = 6

    private val CODE_CHARS = Regex("^[A-Za-z0-9-]+$")
    private val PIN_CHARS = Regex("^[0-9]+$")

    fun normalizeCode(raw: String): String = raw.trim().uppercase()

    fun codeError(code: String): FieldError? = when {
        code.isEmpty() -> FieldError.CODE_REQUIRED
        code.length < MIN_CODE_LENGTH -> FieldError.CODE_TOO_SHORT
        code.length > MAX_CODE_LENGTH || !CODE_CHARS.matches(code) -> FieldError.CODE_INVALID_CHARS
        else -> null
    }

    fun pinError(pin: String): FieldError? =
        if (pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && PIN_CHARS.matches(pin)) null
        else FieldError.PIN_INVALID

    fun validateRegistration(name: String, code: String, pin: String) = FieldErrors(
        name = if (name.isBlank()) FieldError.NAME_REQUIRED else null,
        code = codeError(code),
        pin = pinError(pin)
    )

    fun validateLogin(code: String, pin: String) = FieldErrors(
        code = if (code.isEmpty()) FieldError.CODE_REQUIRED else null,
        pin = pinError(pin)
    )

    fun remainingLockMillis(lockedUntil: Long, now: Long): Long = (lockedUntil - now).coerceAtLeast(0L)

    /** Devuelve (intentosFallidos, bloqueadoHasta) tras un fallo. Bloquea al llegar al 5.º. */
    fun afterFailure(failedAttempts: Int, lockedUntil: Long, now: Long): Pair<Int, Long> {
        val failed = failedAttempts + 1
        return if (failed >= MAX_FAILED_ATTEMPTS) failed to (now + LOCK_MILLIS) else failed to lockedUntil
    }
}