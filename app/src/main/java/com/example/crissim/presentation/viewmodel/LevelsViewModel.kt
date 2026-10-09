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
import com.example.crissim.domain.model.Scenario
import com.example.crissim.domain.model.ScenarioType
import com.example.crissim.domain.usecase.CheckLevelUnlockedUseCase
import com.example.crissim.domain.usecase.GetAllProgressUseCase
import com.example.crissim.domain.usecase.GetAttemptHistoryUseCase
import com.example.crissim.domain.usecase.GetScenariosUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Un escenario listo para mostrar en la lista de niveles. */
data class ScenarioItemUi(
    val id: String,
    val title: String,
    val description: String,
    val type: ScenarioType,
    val maxScore: Int,
    val passingPercent: Int,
    val maxAttempts: Int,
    val attemptsUsed: Int,
    val bestScore: Int?,
    val isPassed: Boolean
)

/** Un nivel con sus escenarios. */
data class LevelSectionUi(
    val level: Level,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val scenarios: List<ScenarioItemUi>
)

/** Estados posibles de la pantalla Niveles. */
sealed interface LevelsUiState {
    data object Loading : LevelsUiState
    data object NeedsUser : LevelsUiState
    data class Ready(val sections: List<LevelSectionUi>) : LevelsUiState
}

/**
 * ViewModel de la pantalla Niveles.
 * Reúne escenarios, progreso, desbloqueo e intentos de cada escenario.
 */
class LevelsViewModel(
    private val userRepository: UserRepository,
    progressRepository: ProgressRepository,
    scenarioRepository: ScenarioRepository
) : ViewModel() {

    private val getScenarios = GetScenariosUseCase(scenarioRepository)
    private val getAllProgress = GetAllProgressUseCase(progressRepository)
    private val checkLevelUnlocked = CheckLevelUnlockedUseCase(progressRepository)
    private val getAttemptHistory = GetAttemptHistoryUseCase(progressRepository)

    private val _state = MutableStateFlow<LevelsUiState>(LevelsUiState.Loading)
    val state: StateFlow<LevelsUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /** Recarga la lista desde la base de datos local. */
    fun refresh() {
        viewModelScope.launch {
            _state.value = loadState()
        }
    }

    private suspend fun loadState(): LevelsUiState {
        val user = userRepository.getActiveUser() ?: return LevelsUiState.NeedsUser

        val scenarios = withContext(Dispatchers.IO) { getScenarios.execute() }
        val progressByLevel = getAllProgress.execute(user.id).associateBy { it.level }

        val sections = Level.entries.map { level ->
            LevelSectionUi(
                level = level,
                isUnlocked = checkLevelUnlocked.execute(user.id, level),
                isCompleted = progressByLevel[level]?.isCompleted == true,
                scenarios = scenarios
                    .filter { it.level == level }
                    .map { toItem(user.id, it) }
            )
        }
        return LevelsUiState.Ready(sections)
    }

    private suspend fun toItem(userId: Long, scenario: Scenario): ScenarioItemUi {
        val history = getAttemptHistory.execute(userId, scenario.id)
        return ScenarioItemUi(
            id = scenario.id,
            title = scenario.title.get(),
            description = scenario.description.get(),
            type = scenario.type,
            maxScore = scenario.calculateMaxScore(),
            passingPercent = scenario.passingScore,
            maxAttempts = scenario.maxAttempts,
            attemptsUsed = history.size,
            bestScore = history.maxOfOrNull { it.score },
            isPassed = history.any { it.isPassed }
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = CrisisSimApp.instance.container
                LevelsViewModel(
                    container.userRepository,
                    container.progressRepository,
                    container.scenarioRepository
                )
            }
        }
    }
}