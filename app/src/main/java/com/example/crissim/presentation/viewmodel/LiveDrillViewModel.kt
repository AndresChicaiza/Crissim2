package com.example.crissim.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.crissim.CrisisSimApp
import com.example.crissim.data.repository.LiveDrillRepository
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.domain.model.DrillIncident
import com.example.crissim.domain.model.DrillStep
import com.example.crissim.domain.model.LiveDrill
import com.example.crissim.domain.model.UserRole
import com.example.crissim.domain.usecase.FinishLiveDrillUseCase
import com.example.crissim.domain.usecase.GetDrillGuideUseCase
import com.example.crissim.domain.usecase.StartLiveDrillUseCase
import com.example.crissim.util.SimulatedAlarmPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la UI para el Simulacro en Vivo (HU-25). */
sealed interface LiveDrillUiState {
    data object Idle : LiveDrillUiState
    data class Active(
        val drill: LiveDrill,
        val steps: List<DrillStep>,
        val currentStepIndex: Int,
        val elapsedSeconds: Long,
        val isMuted: Boolean,
        val incidents: List<DrillIncident>
    ) : LiveDrillUiState
    data class Completed(
        val report: LiveDrill
    ) : LiveDrillUiState
}

/**
 * ViewModel que gestiona la alarma simulada, la guía paso a paso y el cronómetro de evacuación (HU-25).
 */
class LiveDrillViewModel(
    private val userRepository: UserRepository,
    private val liveDrillRepository: LiveDrillRepository,
    val alarmPlayer: SimulatedAlarmPlayer = SimulatedAlarmPlayer()
) : ViewModel() {

    private val getGuide = GetDrillGuideUseCase()
    private val startDrill = StartLiveDrillUseCase()
    private val finishDrill = FinishLiveDrillUseCase(liveDrillRepository)

    private val _state = MutableStateFlow<LiveDrillUiState>(LiveDrillUiState.Idle)
    val state: StateFlow<LiveDrillUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    /** Inicia el simulacro en vivo con sirena, cronómetro y pasos del rol correspondiente. */
    fun startLiveDrill(selectedRole: UserRole? = null) {
        viewModelScope.launch {
            val user = userRepository.getActiveUser()
            val role = selectedRole ?: user?.role ?: UserRole.TEACHER
            val name = user?.name ?: "Docente ILC"
            val userId = user?.id ?: 1L

            val drill = startDrill.execute(userId, name, role)
            val steps = getGuide.execute(role)

            alarmPlayer.startAlarm()

            _state.value = LiveDrillUiState.Active(
                drill = drill,
                steps = steps,
                currentStepIndex = 0,
                elapsedSeconds = 0L,
                isMuted = alarmPlayer.isMuted(),
                incidents = emptyList()
            )

            startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val current = _state.value as? LiveDrillUiState.Active ?: break
                _state.value = current.copy(elapsedSeconds = current.elapsedSeconds + 1)
            }
        }
    }

    /** Silencia o activa el sonido de la sirena sin detener la guía ni el cronómetro (CA-4). */
    fun toggleMuteAlarm() {
        val current = _state.value as? LiveDrillUiState.Active ?: return
        val newMuted = !current.isMuted
        alarmPlayer.setMuted(newMuted)
        _state.value = current.copy(isMuted = newMuted)
    }

    fun nextStep() {
        val current = _state.value as? LiveDrillUiState.Active ?: return
        if (current.currentStepIndex < current.steps.size - 1) {
            _state.value = current.copy(currentStepIndex = current.currentStepIndex + 1)
        }
    }

    fun previousStep() {
        val current = _state.value as? LiveDrillUiState.Active ?: return
        if (current.currentStepIndex > 0) {
            _state.value = current.copy(currentStepIndex = current.currentStepIndex - 1)
        }
    }

    /** Registra una observación/incidencia durante el simulacro (CA-5). */
    fun recordIncident(note: String) {
        val cleanNote = note.trim()
        if (cleanNote.isEmpty()) return
        val current = _state.value as? LiveDrillUiState.Active ?: return
        val currentStep = current.steps.getOrNull(current.currentStepIndex)
        val incident = DrillIncident(
            timestamp = System.currentTimeMillis(),
            stepTitle = currentStep?.title ?: "Paso ${current.currentStepIndex + 1}",
            note = cleanNote
        )
        val updatedIncidents = current.incidents + incident
        _state.value = current.copy(incidents = updatedIncidents)
    }

    /** Finaliza el simulacro, detiene la alarma y genera el reporte (CA-3, CA-5). */
    fun finishDrillNow() {
        val current = _state.value as? LiveDrillUiState.Active ?: return
        timerJob?.cancel()
        alarmPlayer.stopAlarm()

        viewModelScope.launch {
            val report = finishDrill.execute(current.drill, current.incidents)
            _state.value = LiveDrillUiState.Completed(report)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        alarmPlayer.stopAlarm()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = CrisisSimApp.instance.container
                LiveDrillViewModel(
                    container.userRepository,
                    container.liveDrillRepository
                )
            }
        }
    }
}
