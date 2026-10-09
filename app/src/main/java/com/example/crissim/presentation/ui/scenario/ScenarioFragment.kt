package com.example.crissim.presentation.ui.scenario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.crissim.R
import com.example.crissim.databinding.FragmentScenarioBinding
import com.example.crissim.databinding.ItemOptionBinding
import com.example.crissim.domain.model.EndType
import com.example.crissim.presentation.viewmodel.AnswerUi
import com.example.crissim.presentation.viewmodel.FeedbackUi
import com.example.crissim.presentation.viewmodel.NodeUi
import com.example.crissim.presentation.viewmodel.ResultUi
import com.example.crissim.presentation.viewmodel.ScenarioError
import com.example.crissim.presentation.viewmodel.ScenarioUiState
import com.example.crissim.presentation.viewmodel.ScenarioViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Pantalla donde se juega un escenario: decisión con tiempo límite,
 * retroalimentación inmediata con su fuente y resultado final.
 */
class ScenarioFragment : Fragment() {

    private var _binding: FragmentScenarioBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScenarioViewModel by viewModels {
        ScenarioViewModel.factory(requireArguments().getString(ARG_SCENARIO_ID).orEmpty())
    }

    private lateinit var backCallback: OnBackPressedCallback
    private var exitDialogShowing = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScenarioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applySystemBarsInset()
        setupBackHandling()
        setupButtons()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        if (!exitDialogShowing) viewModel.resumeTimer()
    }

    override fun onStop() {
        super.onStop()
        viewModel.pauseTimer() // el tiempo no corre si la app pasa a segundo plano
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── Inicialización ───

    /** Esta pantalla no tiene barra inferior: se respetan ambas barras del sistema. */
    private fun applySystemBarsInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }
    }

    private fun setupBackHandling() {
        backCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                onExitRequested()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
    }

    private fun setupButtons() {
        binding.btnExit.setOnClickListener { onExitRequested() }
        binding.btnErrorBack.setOnClickListener { leaveScenario() }
        binding.btnBackLevels.setOnClickListener { leaveScenario() }
        binding.btnContinue.setOnClickListener { viewModel.continueAfterAnswer() }
        binding.btnRetry.setOnClickListener { viewModel.retry() }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch { viewModel.title.collect { binding.tvScenarioTitle.text = it } }
                launch { viewModel.secondsLeft.collect { updateTimer(it) } }
            }
        }
    }

    // ─── Salir ───

    private fun onExitRequested() {
        val state = viewModel.state.value
        val playing = state is ScenarioUiState.Question || state is ScenarioUiState.Answered
        if (!playing) {
            leaveScenario()
            return
        }
        viewModel.pauseTimer()
        exitDialogShowing = true
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.scenario_exit_title)
            .setMessage(R.string.scenario_exit_message)
            .setPositiveButton(R.string.scenario_exit_confirm) { _, _ -> leaveScenario() }
            .setNegativeButton(R.string.scenario_exit_cancel, null)
            .setOnDismissListener {
                exitDialogShowing = false
                viewModel.resumeTimer()
            }
            .show()
    }

    private fun leaveScenario() {
        findNavController().popBackStack()
    }

    // ─── Dibujo de la pantalla ───

    private fun render(state: ScenarioUiState) {
        binding.groupLoading.isVisible = state is ScenarioUiState.Loading
        binding.groupError.isVisible = state is ScenarioUiState.Failed
        binding.groupQuestion.isVisible = state is ScenarioUiState.Question
        binding.groupAnswered.isVisible = state is ScenarioUiState.Answered
        binding.groupResult.isVisible = state is ScenarioUiState.Finished
        backCallback.isEnabled =
            state is ScenarioUiState.Question || state is ScenarioUiState.Answered

        val timeLimit = (state as? ScenarioUiState.Question)?.node?.timeLimitSeconds
        binding.groupTimer.isVisible = timeLimit != null
        if (timeLimit != null) {
            binding.progressTimer.max = timeLimit
            updateTimer(viewModel.secondsLeft.value)
        }

        when (state) {
            ScenarioUiState.Loading -> Unit
            is ScenarioUiState.Failed -> renderFailed(state.kind)
            is ScenarioUiState.Question -> renderQuestion(state.node)
            is ScenarioUiState.Answered -> renderAnswer(state.answer)
            is ScenarioUiState.Finished -> renderResult(state.result)
        }
        binding.scroll.scrollTo(0, 0)
    }

    private fun updateTimer(seconds: Int) {
        binding.tvTimer.text = getString(R.string.scenario_timer, seconds)
        binding.progressTimer.setProgressCompat(seconds, true)
    }

    private fun renderFailed(kind: ScenarioError) {
        binding.tvError.setText(
            when (kind) {
                ScenarioError.NO_USER -> R.string.scenario_error_no_user
                ScenarioError.NOT_FOUND -> R.string.scenario_error_not_found
            }
        )
    }

    private fun renderQuestion(node: NodeUi) {
        binding.tvStep.text = getString(R.string.scenario_step, node.step)
        binding.tvNarrative.text = node.narrative
        binding.optionsContainer.removeAllViews()
        node.options.forEach { option ->
            val button = ItemOptionBinding.inflate(layoutInflater, binding.optionsContainer, false)
            button.root.text = option.text
            button.root.setOnClickListener { viewModel.selectOption(option.id) }
            binding.optionsContainer.addView(button.root)
        }
    }

    private fun renderAnswer(answer: AnswerUi) {
        val feedback = answer.feedback
        binding.tvVerdict.setText(
            when {
                answer.wasTimeout -> R.string.scenario_verdict_timeout
                feedback?.isCorrect == true -> R.string.scenario_verdict_correct
                feedback?.isCorrect == false -> R.string.scenario_verdict_incorrect
                else -> R.string.scenario_verdict_neutral
            }
        )
        showFeedback(feedback, binding.tvFeedbackText, binding.tvFeedbackSource)
        binding.tvFeedbackScore.text =
            getString(R.string.scenario_score_change, signed(answer.scoreChange))
        binding.btnContinue.setText(
            if (answer.isLast) R.string.scenario_see_result else R.string.scenario_continue
        )
    }

    private fun renderResult(result: ResultUi) {
        binding.tvResultTitle.setText(
            if (result.isPassed) R.string.scenario_result_title_passed
            else R.string.scenario_result_title_failed
        )
        binding.tvResultScore.text = getString(
            R.string.scenario_result_score, result.score, result.maxScore, result.percent
        )
        binding.tvResultRequired.text =
            getString(R.string.scenario_result_required, result.passingPercent)
        binding.tvResultEnd.text =
            getString(R.string.scenario_result_end, getString(endTypeRes(result.endType)))
        showFeedback(result.finalFeedback, binding.tvResultFeedback, binding.tvResultSource)
        binding.tvResultStats.text = getString(
            R.string.scenario_result_stats,
            result.decisionsCount,
            result.timeoutsCount,
            result.durationSeconds,
            result.attemptsUsed
        )
        binding.tvResultNotSaved.isVisible = !result.saved
    }

    // ─── Textos ───

    private fun showFeedback(
        feedback: FeedbackUi?,
        textView: android.widget.TextView,
        sourceView: android.widget.TextView
    ) {
        textView.isVisible = feedback != null
        textView.text = feedback?.text.orEmpty()
        val source = feedback?.source
        sourceView.isVisible = source != null
        sourceView.text = source?.let { getString(R.string.scenario_source, it) }.orEmpty()
    }

    private fun signed(value: Int): String = if (value > 0) "+$value" else value.toString()

    private fun endTypeRes(type: EndType): Int = when (type) {
        EndType.SUCCESS -> R.string.scenario_end_success
        EndType.PARTIAL_SUCCESS -> R.string.scenario_end_partial
        EndType.FAILURE -> R.string.scenario_end_failure
    }

    companion object {
        const val ARG_SCENARIO_ID = "scenarioId"
    }
}