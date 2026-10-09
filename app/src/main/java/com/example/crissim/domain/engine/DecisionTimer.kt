package com.example.crissim.domain.engine

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Temporizador para las decisiones con límite de tiempo (RF-02).
 * Kotlin puro + Coroutines. Sin dependencias Android.
 *
 * Emite el tiempo restante cada segundo a través de un StateFlow.
 * Cuando el tiempo se agota, invoca el callback onTimeout.
 */
class DecisionTimer(
    private val scope: CoroutineScope
) {
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null
    private var onTimeoutCallback: (() -> Unit)? = null

    /**
     * Inicia la cuenta regresiva.
     *
     * @param seconds Tiempo límite en segundos.
     * @param onTimeout Callback cuando el tiempo se agota.
     */
    fun start(seconds: Int, onTimeout: () -> Unit) {
        stop()
        onTimeoutCallback = onTimeout
        _remainingSeconds.value = seconds
        _isRunning.value = true

        timerJob = scope.launch {
            for (remaining in seconds downTo 1) {
                _remainingSeconds.value = remaining
                delay(1000L)
            }
            _remainingSeconds.value = 0
            _isRunning.value = false
            onTimeoutCallback?.invoke()
        }
    }

    /**
     * Detiene el temporizador (cuando el usuario decide antes del timeout).
     */
    fun stop() {
        timerJob?.cancel()
        timerJob = null
        _isRunning.value = false
        onTimeoutCallback = null
    }

    /**
     * Pausa el temporizador (ej: al mostrar feedback).
     */
    fun pause() {
        timerJob?.cancel()
        timerJob = null
        // Mantiene _isRunning en true para indicar que hay timer activo
    }

    /**
     * Reanuda el temporizador con el tiempo restante.
     */
    fun resume(onTimeout: () -> Unit) {
        if (_isRunning.value && _remainingSeconds.value > 0) {
            start(_remainingSeconds.value, onTimeout)
        }
    }
}


