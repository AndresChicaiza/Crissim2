package com.example.crissim.domain.engine

import android.content.Context
import android.media.MediaPlayer
import android.os.Vibrator
import android.provider.Settings
import com.example.crissim.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Temporizador para las decisiones con límite de tiempo (RF-02).
 * Kotlin puro + Coroutines. Sin dependencias Android (excepto callbacks opcionales).
 *
 * Emite el tiempo restante cada segundo a través de un StateFlow.
 * Cuando el tiempo se agota, invoca el callback onTimeout.
 * Opcionalmente reproduce sonido y vibración en eventos clave.
 */
class DecisionTimer(
    private val scope: CoroutineScope,
    private val context: Context? = null
) {
    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private var timerJob: Job? = null
    private var onTimeoutCallback: (() -> Unit)? = null
    private var generation: Long = 0
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    private var soundEnabled = true
    private var vibrationEnabled = true

    init {
        if (context != null) {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }

    /**
     * Actualiza la configuración de accesibilidad (se llama antes de start).
     */
    fun updateAccessibilitySettings(soundEnabled: Boolean, vibrationEnabled: Boolean) {
        this.soundEnabled = soundEnabled
        this.vibrationEnabled = vibrationEnabled
    }

    /**
     * Inicia la cuenta regresiva.
     *
     * @param seconds Tiempo límite en segundos.
     * @param onTimeout Callback cuando el tiempo se agota.
     */
    fun start(seconds: Int, onTimeout: () -> Unit) {
        // Leer preferencias actuales cada vez que se inicia
        if (context != null) {
            val prefs = context.getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)
            soundEnabled = prefs.getBoolean("sound_enabled", true)
            vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        }

        stop()
        val currentGeneration = ++generation
        onTimeoutCallback = onTimeout
        _remainingSeconds.value = seconds
        _isRunning.value = true

        // Sonido de inicio
        playTickSound()

        timerJob = scope.launch {
            for (remaining in seconds downTo 1) {
                if (currentGeneration != generation) return@launch
                _remainingSeconds.value = remaining
                
                // Sonido de tick cada segundo (opcional, solo últimos 5 segundos)
                if (remaining <= 5 && remaining > 0) {
                    playTickSound()
                    vibrateShort()
                }
                
                delay(1000L)
            }
            if (currentGeneration != generation) return@launch
            _remainingSeconds.value = 0
            _isRunning.value = false
            
            // Sonido y vibración de timeout
            playTimeoutSound()
            vibrateLong()
            
            val callback = onTimeoutCallback
            onTimeoutCallback = null
            callback?.invoke()
        }
    }

    /**
     * Detiene el temporizador (cuando el usuario decide antes del timeout).
     */
    fun stop() {
        generation++
        timerJob?.cancel()
        timerJob = null
        _isRunning.value = false
        onTimeoutCallback = null
        releaseMediaPlayer()
    }

    /**
     * Pausa el temporizador (ej: al mostrar feedback).
     */
    fun pause() {
        generation++
        timerJob?.cancel()
        timerJob = null
        // Mantiene _isRunning en true para indicar que hay timer activo
    }

    /**
     * Reanuda el temporizador con el tiempo restante.
     */
    fun resume(onTimeout: () -> Unit) {
        if (_isRunning.value && _remainingSeconds.value > 0 && timerJob?.isActive != true) {
            start(_remainingSeconds.value, onTimeout)
        }
    }

    // ─── Sonido y Vibración ───

    private fun playTickSound() {
        if (!soundEnabled || context == null) return
        try {
            // Usar sonido del sistema para tick
            val uri = Settings.System.DEFAULT_NOTIFICATION_URI
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, uri)
            mediaPlayer?.isLooping = false
            mediaPlayer?.setVolume(0.5f, 0.5f)
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Ignorar errores de sonido
        }
    }

    private fun playTimeoutSound() {
        if (!soundEnabled || context == null) return
        try {
            // Sonido más distintivo para timeout
            val uri = Settings.System.DEFAULT_ALARM_ALERT_URI
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, uri)
            mediaPlayer?.isLooping = false
            mediaPlayer?.setVolume(0.7f, 0.7f)
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Ignorar errores de sonido
        }
    }

    private fun vibrateShort() {
        if (!vibrationEnabled || vibrator == null) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignorar errores de vibración
        }
    }

    private fun vibrateLong() {
        if (!vibrationEnabled || vibrator == null) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 200, 100, 200), -1)
            }
        } catch (e: Exception) {
            // Ignorar errores de vibración
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    /**
     * Vibración para respuesta incorrecta (patrón distintivo: dos pulsos cortos).
     */
    fun vibrateWrong() {
        if (!vibrationEnabled || vibrator == null) return
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator?.vibrate(android.os.VibrationEffect.createWaveform(longArrayOf(0, 100, 50, 100), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 100, 50, 100), -1)
            }
        } catch (e: Exception) {
            // Ignorar errores de vibración
        }
    }

    /**
     * Reproduce el sonido de respuesta correcta desde la carpeta raw.
     */
    fun playCorrectSound() {
        if (!soundEnabled || context == null) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context, R.raw.dragon_studio_correct)
            mediaPlayer?.isLooping = false
            mediaPlayer?.setVolume(0.7f, 0.7f)
            mediaPlayer?.start()
        } catch (e: Exception) {
            // Ignorar errores de sonido
        }
    }
}


