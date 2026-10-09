package com.example.crissim.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.crissim.CrisisSimApp
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.usecase.CheckLevelUnlockedUseCase
import com.example.crissim.domain.usecase.GetAllProgressUseCase
import com.example.crissim.domain.usecase.GetScenariosUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Datos de un nivel listos para mostrar en pantalla. */
data class LevelUi(
    val level: Level,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val bestScore: Int,
    val maxScore: Int,
    val attempts: Int
) {
    val percent: Int
        get() = if (maxScore > 0) (bestScore * 100 / maxScore).coerceIn(0, 100) else 0
}

/** Estados posibles de la pantalla Inicio. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NeedsName : HomeUiState
    data class Ready(
        val userName: String,
        val levels: List<LevelUi>,
        val scenarioCount: Int
    ) : HomeUiState
}

/**
 * ViewModel de la pantalla Inicio.
 * Decide qué mostrar: pedir el nombre (primera vez) o el progreso por niveles.
 */
class HomeViewModel(
    private val userRepository: UserRepository,
    progressRepository: ProgressRepository,
    scenarioRepository: ScenarioRepository
) : ViewModel() {

    private val getAllProgress = GetAllProgressUseCase(progressRepository)
    private val checkLevelUnlocked = CheckLevelUnlockedUseCase(progressRepository)
    private val getScenarios = GetScenariosUseCase(scenarioRepository)

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Recarga el estado desde la base de datos local. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = loadState()
        }
    }

    /** Guarda el nombre del usuario (primera vez) y muestra el progreso. */
    fun saveName(name: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            userRepository.createUser(cleanName)
            _state.value = loadState()
        }
    }

    private suspend fun loadState(): HomeUiState {
        val user = userRepository.getActiveUser() ?: return HomeUiState.NeedsName

        val levels = getAllProgress.execute(user.id).map { progress ->
            LevelUi(
                level = progress.level,
                isUnlocked = checkLevelUnlocked.execute(user.id, progress.level),
                isCompleted = progress.isCompleted,
                bestScore = progress.bestScore,
                maxScore = progress.maxScore,
                attempts = progress.attempts
            )
        }
        val scenarioCount = withContext(Dispatchers.IO) { getScenarios.execute().size }

        return HomeUiState.Ready(user.name, levels, scenarioCount)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = CrisisSimApp.instance.container
                HomeViewModel(
                    container.userRepository,
                    container.progressRepository,
                    container.scenarioRepository
                )
            }
        }
    }
}