package com.example.crissim.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Reproductor de alarma simulada de emergencia 100% offline (HU-25).
 * Usa ToneGenerator interno sin requerir archivos de audio ni conexión.
 */
class SimulatedAlarmPlayer {

    private var toneGenerator: ToneGenerator? = null
    private var isPlaying = false
    private var isMuted = false
    private var alarmThread: Thread? = null

    /** Inicia la sirena/alarma simulada. */
    fun startAlarm() {
        if (isPlaying) return
        isPlaying = true
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo crear ToneGenerator", e)
        }

        alarmThread = Thread {
            while (isPlaying) {
                if (!isMuted && toneGenerator != null) {
                    try {
                        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 400)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error reproduciendo tono de alarma", e)
                    }
                }
                try {
                    Thread.sleep(600)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }.apply { start() }
    }

    /** Silencia o activa el sonido de la alarma manteniendo la guía activa. */
    fun setMuted(muted: Boolean) {
        this.isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    /** Detiene la alarma por completo. */
    fun stopAlarm() {
        isPlaying = false
        alarmThread?.interrupt()
        alarmThread = null
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error liberando ToneGenerator", e)
        }
        toneGenerator = null
    }

    companion object {
        private const val TAG = "SimulatedAlarmPlayer"
    }
}
