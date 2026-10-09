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
import com.example.crissim.domain.engine.DecisionTimer
import com.example.crissim.domain.engine.OptionResult
import com.example.crissim.domain.engine.ScenarioEngine
import com.example.crissim.domain.model.EndType
import com.example.crissim.domain.model.Feedback
import com.example.crissim.domain.model.Scenario
import com.example.crissim.domain.model.ScenarioNode
import com.example.crissim.domain.usecase.GetAttemptHistoryUseCase
import com.example.crissim.domain.usecase.GetScenarioByIdUseCase
import com.example.crissim.domain.usecase.SaveAttemptResultUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─── Modelos de pantalla ───

data class OptionUi(val id: String, val text: String)

data class NodeUi(
    val step: Int,
    val narrative: String,
    val options: List<OptionUi>,
    val timeLimitSeconds: Int?
)

data class FeedbackUi(
    val text: String,
    val isCorrect: Boolean,
    val source: String?
)

data class AnswerUi(
    val feedback: FeedbackUi?,
    val scoreChange: Int,
    val wasTimeout: Boolean,
    val isLast: Boolean
)

data class ResultUi(
    val score: Int,
    val maxScore: Int,
    val percent: Int,
    val passingPercent: Int,
    val isPassed: Boolean,
    val endType: EndType,
    val finalFeedback: FeedbackUi?,
    val decisionsCount: Int,
    val timeoutsCount: Int,
    val durationSeconds: Long,
    val attemptsUsed: Int,
    val saved: Boolean
)

enum class ScenarioError { NO_USER, NOT_FOUND }

sealed interface ScenarioUiState {
    data object Loading : ScenarioUiState
    data class Failed(val kind: ScenarioError) : ScenarioUiState
    data class Question(val node: NodeUi) : ScenarioUiState
    data class Answered(val answer: AnswerUi) : ScenarioUiState
    data class Finished(val result: ResultUi) : ScenarioUiState
}

/**
 * ViewModel de la pantalla del escenario.
 * Coordina el motor (decisiones y puntaje), el temporizador (RF-02)
 * y el guardado del resultado al terminar (RF-05).
 */
class ScenarioViewModel(
    private val scenarioId: String,
    private val userRepository: UserRepository,
    progressRepository: ProgressRepository,
    scenarioRepository: ScenarioRepository
) : ViewModel() {

    private val getScenarioById = GetScenarioByIdUseCase(scenarioRepository)
    private val getAttemptHistory = GetAttemptHistoryUseCase(progressRepository)
    private val saveAttemptResult = SaveAttemptResultUseCase(progressRepository)

    private val engine = ScenarioEngine()
    private val timer = DecisionTimer(viewModelScope)

    private val _state = MutableStateFlow<ScenarioUiState>(ScenarioUiState.Loading)
    val state: StateFlow<ScenarioUiState> = _state.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    /** Segundos que quedan en la decisión actual. */
    val secondsLeft: StateFlow<Int> = timer.remainingSeconds

    private var scenario: Scenario? = null
    private var userId: Long = 0L
    private var pendingResult: OptionResult? = null
    private var finalFeedback: Feedback? = null

    init {
        load()
    }

    // ─── Acciones de la pantalla ───

    /** El usuario elige una opción del nodo actual. */
    fun selectOption(optionId: String) {
        if (_state.value !is ScenarioUiState.Question) return
        timer.stop()
        val result = engine.selectOption(optionId) ?: return
        showAnswer(result)
    }

    /** Pasa de la retroalimentación al siguiente nodo o al resultado final. */
    fun continueAfterAnswer() {
        val result = pendingResult ?: return
        pendingResult = null
        if (result.isScenarioEnd) finishScenario() else showCurrentNode()
    }

    /** Empieza un intento nuevo del mismo escenario. */
    fun retry() {
        startAttempt()
    }

    fun pauseTimer() {
        timer.pause()
    }

    fun resumeTimer() {
        if (_state.value is ScenarioUiState.Question) {
            timer.resume { onTimeout() }
        }
    }

    // ─── Flujo interno ───

    private fun load() {
        viewModelScope.launch {
            val user = userRepository.getActiveUser()
            if (user == null) {
                _state.value = ScenarioUiState.Failed(ScenarioError.NO_USER)
                return@launch
            }
            val loaded = withContext(Dispatchers.IO) { getScenarioById.execute(scenarioId) }
            if (loaded == null) {
                _state.value = ScenarioUiState.Failed(ScenarioError.NOT_FOUND)
                return@launch
            }
            userId = user.id
            scenario = loaded
            _title.value = loaded.title.get()
            startAttempt()
        }
    }

    private fun startAttempt() {
        val current = scenario ?: return
        timer.stop()
        pendingResult = null
        finalFeedback = null
        engine.startScenario(current)
        showCurrentNode()
    }

    private fun showCurrentNode() {
        val node = engine.state.currentNode
        if (node == null) {
            _state.value = ScenarioUiState.Failed(ScenarioError.NOT_FOUND)
            return
        }
        val limit = node.timeLimitSeconds?.takeIf { it > 0 }
        if (limit != null) timer.start(limit) { onTimeout() }
        _state.value = ScenarioUiState.Question(node.toUi(engine.state.decisions.size + 1))
    }

    private fun onTimeout() {
        if (_state.value !is ScenarioUiState.Question) return
        // Sin opción por defecto, el usuario puede seguir respondiendo sin límite.
        val result = engine.handleTimeout() ?: return
        showAnswer(result)
    }

    private fun showAnswer(result: OptionResult) {
        pendingResult = result
        if (result.isScenarioEnd) finalFeedback = result.finalFeedback
        _state.value = ScenarioUiState.Answered(
            AnswerUi(
                feedback = result.feedback?.toUi(),
                scoreChange = result.scoreChange,
                wasTimeout = result.wasTimeout,
                isLast = result.isScenarioEnd
            )
        )
    }

    private fun finishScenario() {
        viewModelScope.launch {
            val current = scenario ?: return@launch
            val result = engine.getAttemptResult(userId)
            if (result == null) {
                _state.value = ScenarioUiState.Failed(ScenarioError.NOT_FOUND)
                return@launch
            }

            var saved = true
            try {
                saveAttemptResult.execute(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                saved = false // se muestra el resultado aunque no se haya podido guardar
            }

            val percent = if (result.maxScore > 0) {
                (result.score * 100 / result.maxScore).coerceIn(0, 100)
            } else {
                0
            }
            _state.value = ScenarioUiState.Finished(
                ResultUi(
                    score = result.score,
                    maxScore = result.maxScore,
                    percent = percent,
                    passingPercent = current.passingScore,
                    isPassed = result.isPassed,
                    endType = result.endType,
                    finalFeedback = finalFeedback?.toUi(),
                    decisionsCount = result.decisions.size,
                    timeoutsCount = result.decisions.count { it.wasTimeout },
                    durationSeconds = result.durationSeconds,
                    attemptsUsed = getAttemptHistory.execute(userId, current.id).size,
                    saved = saved
                )
            )
        }
    }

    // ─── Conversión a modelos de pantalla ───

    private fun ScenarioNode.toUi(step: Int) = NodeUi(
        step = step,
        narrative = narrative.get(),
        options = options.map { OptionUi(it.id, it.text.get()) },
        timeLimitSeconds = timeLimitSeconds?.takeIf { it > 0 }
    )

    private fun Feedback.toUi() = FeedbackUi(
        text = text.get(),
        isCorrect = isCorrect,
        source = listOfNotNull(protocolSource.takeIf { it.isNotBlank() }, protocolRef)
            .joinToString(" · ")
            .ifBlank { null }
    )

    companion object {
        fun factory(scenarioId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = CrisisSimApp.instance.container
                ScenarioViewModel(
                    scenarioId,
                    container.userRepository,
                    container.progressRepository,
                    container.scenarioRepository
                )
            }
        }
    }
}