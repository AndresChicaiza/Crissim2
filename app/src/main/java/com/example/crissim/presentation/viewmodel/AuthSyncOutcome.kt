package com.example.crissim.presentation.viewmodel

import com.google.firebase.firestore.FirebaseFirestoreException

/** A Firestore failure occurs after authentication and must keep the local session usable. */
internal object AuthSyncOutcome {
    fun afterSyncFailure(error: Exception, isRegistration: Boolean): AuthUiState.SignedIn {
        val code = (error as? FirebaseFirestoreException)?.code?.name
        return afterSyncFailureCode(code, isRegistration)
    }

    internal fun afterSyncFailureCode(firestoreCode: String?, isRegistration: Boolean): AuthUiState.SignedIn {
        val accountMessage = if (isRegistration) {
            "Cuenta creada correctamente. "
        } else {
            "Sesión iniciada correctamente. "
        }
        val detail = when (firestoreCode) {
            "PERMISSION_DENIED" ->
                "Firestore rechazó el acceso (PERMISSION_DENIED). Tus datos locales se conservarán; revisa las reglas de Firestore y vuelve a intentar la sincronización."
            "UNAVAILABLE", "DEADLINE_EXCEEDED" ->
                "No se pudo conectar con Firestore. Tus datos locales se conservarán y podrás reintentar la sincronización cuando vuelva la conexión."
            else ->
                "No se pudo sincronizar tu progreso. Tus datos locales se conservarán y podrás reintentar la sincronización más tarde."
        }
        return AuthUiState.SignedIn(accountMessage + detail)
    }
}
