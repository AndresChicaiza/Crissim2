package com.example.crissim.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import android.util.Log
import com.example.crissim.CrisisSimApp
import com.example.crissim.data.repository.impl.LocalAccountBindingException
import com.example.crissim.data.repository.impl.StudentCloudSync
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.domain.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class SignedIn(val warning: String? = null) : AuthUiState
    data class Message(val text: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val auth: FirebaseAuth,
    private val users: UserRepository,
    private val cloudSync: StudentCloudSync
) : ViewModel() {
    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun restoreSession() {
        val user = auth.currentUser ?: return
        if (_state.value !is AuthUiState.Idle) return
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            syncAndEnter(user.uid, user.email?.substringBefore('@').orEmpty())
        }
    }

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).awaitResult()
                val user = result.user ?: error("No se pudo obtener la cuenta autenticada.")
                syncAndEnter(user.uid, user.email?.substringBefore('@').orEmpty())
            } catch (error: Exception) {
                _state.value = AuthUiState.Error(error.toSpanishMessage())
            }
        }
    }

    fun register(name: String, email: String, password: String) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _state.value = AuthUiState.Error("Escribe tu nombre para crear la cuenta.")
            return
        }
        if (!validate(email, password)) return
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val user = try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).awaitResult()
                result.user ?: error("No se pudo crear la cuenta.")
            } catch (error: Exception) {
                _state.value = AuthUiState.Error(error.toSpanishMessage())
                return@launch
            }

            try {
                if (users.getActiveUser() == null) users.createUser(cleanName, UserRole.STUDENT)
            } catch (error: Exception) {
                Log.e(TAG, "Firebase account created but local profile initialization failed", error)
                _state.value = AuthUiState.SignedIn(
                    "Cuenta creada correctamente. No se pudo preparar el perfil local; tus datos locales no se modificaron."
                )
                return@launch
            }
            syncAndEnter(user.uid, cleanName, isRegistration = true)
        }
    }

    fun sendPasswordReset(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _state.value = AuthUiState.Error("Escribe primero el correo de tu cuenta.")
            return
        }
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                auth.sendPasswordResetEmail(email.trim()).awaitResult()
                _state.value = AuthUiState.Message("Te enviamos un enlace para restablecer la contraseña.")
            } catch (error: Exception) {
                _state.value = AuthUiState.Error(error.toSpanishMessage())
            }
        }
    }

    fun consumeMessage() { _state.value = AuthUiState.Idle }

    private suspend fun syncAndEnter(uid: String, fallbackName: String, isRegistration: Boolean = false) {
        try {
            cloudSync.synchronize(uid, fallbackName)
            _state.value = AuthUiState.SignedIn()
        } catch (error: CancellationException) {
            throw error
        } catch (error: LocalAccountBindingException) {
            // A device is deliberately bound to one student's local Room database.
            Log.e(TAG, "Authentication succeeded but this device is bound to another local account", error)
            auth.signOut()
            val prefix = if (isRegistration) "La cuenta Firebase se cre\u00f3, pero " else ""
            _state.value = AuthUiState.Error(prefix + (error.message ?: "la cuenta no coincide con los datos locales."))
        } catch (error: Exception) {
            val firestoreCode = (error as? FirebaseFirestoreException)?.code?.name ?: "not-a-FirestoreException"
            Log.e(TAG, "Firestore synchronization failed (code=$firestoreCode)", error)
            _state.value = AuthSyncOutcome.afterSyncFailure(error, isRegistration)
        }
    }

    private fun validate(email: String, password: String): Boolean {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _state.value = AuthUiState.Error("Escribe un correo válido.")
            return false
        }
        if (password.length < 6) {
            _state.value = AuthUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return false
        }
        return true
    }

    private fun Exception.toSpanishMessage(): String = when {
        message.orEmpty().contains("already in use", true) -> "Ese correo ya tiene una cuenta. Inicia sesión."
        message.orEmpty().contains("no user record", true) -> "No existe una cuenta con ese correo. Regístrate primero."
        message.orEmpty().contains("password is invalid", true) || message.orEmpty().contains("wrong password", true) -> "El correo o la contraseña no son correctos."
        message.orEmpty().contains("network", true) -> "No hay conexión. Revisa internet e inténtalo de nuevo."
        else -> message?.takeIf { it.isNotBlank() } ?: "No se pudo completar la operación. Inténtalo de nuevo."
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    companion object {
        private const val TAG = "AuthViewModel"
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = CrisisSimApp.instance
                AuthViewModel(
                    FirebaseAuth.getInstance(),
                    app.container.userRepository,
                    app.container.studentCloudSync
                )
            }
        }
    }
}
