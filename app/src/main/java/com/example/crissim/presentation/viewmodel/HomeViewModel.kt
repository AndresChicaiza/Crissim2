package com.example.crissim.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.crissim.CrisisSimApp
import com.example.crissim.data.repository.MicroTrainingRepository
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.data.repository.impl.StudentCloudSync
import com.example.crissim.domain.model.Badge
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.model.MicroTrainingWithProgress
import com.example.crissim.domain.model.UserRole
import com.example.crissim.domain.usecase.CheckLevelUnlockedUseCase
import com.example.crissim.domain.usecase.GetAllProgressUseCase
import com.example.crissim.domain.usecase.GetNextMicroTrainingUseCase
import com.example.crissim.domain.usecase.GetScenariosUseCase
import com.example.crissim.domain.usecase.MicroTrainingAnswerResult
import com.example.crissim.domain.usecase.SubmitMicroAnswerUseCase
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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

/** Estado de la UI para micro-entrenamientos y rachas (HU-24). */
data class MicroTrainingUiState(
    val currentStreak: Int = 0,
    val nextMicroTraining: MicroTrainingWithProgress? = null,
    val earnedBadges: List<Badge> = emptyList(),
    val lastResult: MicroTrainingAnswerResult? = null,
    val isPracticedToday: Boolean = false
)

/** Estados posibles de la pantalla Inicio. */
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object NeedsName : HomeUiState
    data class Ready(
        val userName: String,
        val userRole: UserRole,
        val levels: List<LevelUi>,
        val scenarioCount: Int,
        val microTrainingUi: MicroTrainingUiState = MicroTrainingUiState()
    ) : HomeUiState
}

/**
 * ViewModel de la pantalla Inicio.
 * Gestiona el progreso de niveles y los micro-entrenamientos diarios (HU-24).
 */
class HomeViewModel(
    private val userRepository: UserRepository,
    progressRepository: ProgressRepository,
    scenarioRepository: ScenarioRepository,
    private val microTrainingRepository: MicroTrainingRepository,
    private val cloudSync: StudentCloudSync
) : ViewModel() {

    private val getAllProgress = GetAllProgressUseCase(progressRepository)
    private val checkLevelUnlocked = CheckLevelUnlockedUseCase(progressRepository)
    private val getScenarios = GetScenariosUseCase(scenarioRepository)
    private val getNextMicroTraining = GetNextMicroTrainingUseCase(microTrainingRepository)
    private val submitMicroAnswer = SubmitMicroAnswerUseCase(microTrainingRepository)

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    init {
        refresh()
    }

    /** Recarga el estado desde la base de datos local. */
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val account = FirebaseAuth.getInstance().currentUser
            if (account != null) {
                try {
                    cloudSync.synchronize(
                        account.uid,
                        account.displayName ?: account.email?.substringBefore('@').orEmpty()
                    )
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    val code = (error as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name
                    val reason = code ?: if (error is IllegalArgumentException) "local-account-binding" else "unknown"
                    Log.e(TAG, "Automatic sync retry failed (reason=$reason)", error)
                }
            }
            _state.value = loadState()
        }
    }

    /** Envía la respuesta del usuario a la tarjeta de micro-entrenamiento diario. */
    fun submitMicroTrainingAnswer(selectedOptionIndex: Int) {
        val currentState = _state.value as? HomeUiState.Ready ?: return
        val currentMicro = currentState.microTrainingUi.nextMicroTraining ?: return

        viewModelScope.launch {
            val user = userRepository.getActiveUser() ?: return@launch
            val result = submitMicroAnswer.execute(
                userId = user.id,
                microTrainingId = currentMicro.microTraining.id,
                selectedOptionIndex = selectedOptionIndex
            ) ?: return@launch

            val updatedStreak = microTrainingRepository.getStreak(user.id)?.currentStreak ?: result.streak.currentStreak
            val updatedBadges = microTrainingRepository.getUserEarnedBadges(user.id)
            val updatedNextMicro = getNextMicroTraining.execute(user.id)
            val isPracticedToday = isToday(result.streak.lastPracticeDate) && result.isCorrect

            _state.value = currentState.copy(
                microTrainingUi = currentState.microTrainingUi.copy(
                    currentStreak = updatedStreak,
                    earnedBadges = updatedBadges,
                    nextMicroTraining = updatedNextMicro,
                    lastResult = result,
                    isPracticedToday = isPracticedToday
                )
            )
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

        val nextMicro = getNextMicroTraining.execute(user.id)
        val streak = microTrainingRepository.getStreak(user.id)
        val earnedBadges = microTrainingRepository.getUserEarnedBadges(user.id)
        val isPracticedToday = isToday(streak?.lastPracticeDate ?: 0L)

        val microUi = MicroTrainingUiState(
            currentStreak = streak?.currentStreak ?: 0,
            nextMicroTraining = nextMicro,
            earnedBadges = earnedBadges,
            isPracticedToday = isPracticedToday
        )

        return HomeUiState.Ready(user.name, user.role, levels, scenarioCount, microUi)
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp <= 0) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp }
        val cal2 = Calendar.getInstance()
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        private const val TAG = "HomeViewModel"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = CrisisSimApp.instance.container
                HomeViewModel(
                    container.userRepository,
                    container.progressRepository,
                    container.scenarioRepository,
                    container.microTrainingRepository,
                    container.studentCloudSync
                )
            }
        }
    }
}
