package com.example.crissim.presentation.viewmodel

import org.junit.Assert.assertTrue
import org.junit.Test

class AuthSyncOutcomeTest {
    @Test
    fun firestorePermissionFailureAfterRegistrationKeepsAccountSuccessful() {
        // This policy runs only after Firebase Authentication has already returned a user.
        val outcome = AuthSyncOutcome.afterSyncFailureCode("PERMISSION_DENIED", isRegistration = true)

        val warning = requireNotNull(outcome.warning)
        assertTrue(warning.contains("Cuenta creada correctamente"))
        assertTrue(warning.contains("PERMISSION_DENIED"))
        assertTrue(warning.contains("datos locales se conservarán"))
        assertTrue(warning.contains("revisa las reglas de Firestore"))
    }

    @Test
    fun transientSyncFailureKeepsLocalSessionAndCanBeRetried() {
        val outcome = AuthSyncOutcome.afterSyncFailureCode(null, isRegistration = false)

        assertTrue(outcome.warning.orEmpty().contains("Sesión iniciada correctamente"))
        assertTrue(outcome.warning.orEmpty().contains("reintentar la sincronización"))
    }
}
