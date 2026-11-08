package com.example.crissim.presentation.ui.scenario

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
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
import androidx.navigation.navOptions
import com.example.crissim.R
import com.example.crissim.databinding.FragmentScenarioBinding
import com.example.crissim.databinding.ItemOptionBinding
import com.example.crissim.presentation.viewmodel.AnswerUi
import com.example.crissim.presentation.viewmodel.NodeUi
import com.example.crissim.presentation.viewmodel.ScenarioUiState
import com.example.crissim.presentation.viewmodel.ScenarioViewModel
import kotlinx.coroutines.launch

class ScenarioFragment : Fragment() {

    private var _binding: FragmentScenarioBinding? = null
    private val binding get() = _binding!!

    private var scenarioId: String = "sis-01"

    private val viewModel: ScenarioViewModel by viewModels {
        ScenarioViewModel.factory(scenarioId)
    }

    private lateinit var backCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        scenarioId = arguments?.getString(ARG_SCENARIO_ID) ?: "sis-01"
        super.onCreate(savedInstanceState)
    }

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
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeTimer()
    }

    override fun onPause() {
        viewModel.pauseTimer()
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

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
                findNavController().popBackStack()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)
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

    private fun render(state: ScenarioUiState) {
        binding.groupLoading.isVisible = state is ScenarioUiState.Loading
        binding.groupAnswered.isVisible = state is ScenarioUiState.Answered
        binding.overlayDim.isVisible = state is ScenarioUiState.Answered
        backCallback.isEnabled = state is ScenarioUiState.Question || state is ScenarioUiState.Answered

        when (state) {
            ScenarioUiState.Loading -> Unit
            is ScenarioUiState.Failed -> Unit
            is ScenarioUiState.Question -> renderQuestion(state.node)
            is ScenarioUiState.Answered -> renderAnswer(state.answer)
            is ScenarioUiState.Finished -> {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.scenarioFragment) {
                    val result = state.result
                    val correctCount = result.score / 10
                    navController.navigate(
                        R.id.scenarioResultFragment,
                        bundleOf(
                            ARG_SCENARIO_ID to scenarioId,
                            ScenarioResultFragment.ARG_SCENARIO_TITLE to viewModel.title.value,
                            ScenarioResultFragment.ARG_RESULT_PASSED to result.isPassed,
                            ScenarioResultFragment.ARG_RESULT_CORRECT_COUNT to correctCount,
                            ScenarioResultFragment.ARG_RESULT_TOTAL_COUNT to result.decisionsCount,
                            ScenarioResultFragment.ARG_RESULT_PERCENT to result.percent,
                            ScenarioResultFragment.ARG_RESULT_SCORE to result.score,
                            ScenarioResultFragment.ARG_RESULT_MAX_SCORE to result.maxScore,
                            ScenarioResultFragment.ARG_RESULT_DURATION to result.durationSeconds,
                            ScenarioResultFragment.ARG_RESULT_SAVED to result.saved
                        ),
                        navOptions {
                            popUpTo(R.id.scenarioFragment) { inclusive = true }
                            launchSingleTop = true
                        }
                    )
                }
            }
        }
    }

    private fun updateTimer(seconds: Int) {
        binding.tvTimerSeconds.text = seconds.toString()
        binding.progressTimer.setProgressCompat(seconds, true)
    }

    private fun renderQuestion(node: NodeUi) {
        binding.tvStep.text = "${scenarioId.uppercase()} · Decisión ${node.step} de ${node.questionCount}"
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
        val isCorrect = feedback?.isCorrect == true

        if (isCorrect) {
            binding.groupAnswered.strokeColor = Color.parseColor("#176343")
            binding.layoutBadge.setBackgroundResource(R.drawable.bg_pill_correct)
            binding.ivVerdictIcon.setImageResource(R.drawable.ic_check)
            binding.tvBadgeText.text = getString(R.string.action_correct)
            binding.tvBadgeText.setTextColor(Color.parseColor("#176343"))

            binding.tvVerdict.text = getString(R.string.verdict_correct_title)
            binding.tvVerdict.setTextColor(Color.parseColor("#176343"))

            binding.tvChoiceMade.isVisible = answer.wasTimeout
            if (answer.wasTimeout) {
                binding.tvChoiceMade.text = getString(
                    R.string.scenario_timeout_choice,
                    answer.selectedOptionText
                )
            }
            binding.tvFeedbackText.text = feedback?.text.orEmpty().ifBlank { getString(R.string.verdict_correct_desc) }
            binding.tvFeedbackNext.text = getString(R.string.verdict_correct_next)
            binding.tvFeedbackCaption.text = getString(R.string.verdict_timer_paused)

            binding.btnContinue.text = getString(if (answer.isLast) R.string.scenario_see_result else R.string.scenario_continue)
            binding.btnContinue.setOnClickListener {
                viewModel.continueAfterAnswer()
            }
        } else {
            binding.groupAnswered.strokeColor = Color.parseColor("#C5221F")
            binding.layoutBadge.setBackgroundResource(R.drawable.bg_pill_incorrect)
            binding.ivVerdictIcon.setImageResource(R.drawable.ic_close)
            binding.tvBadgeText.text = getString(R.string.action_incorrect)
            binding.tvBadgeText.setTextColor(Color.parseColor("#C5221F"))

            binding.tvVerdict.text = getString(R.string.verdict_incorrect_title)
            binding.tvVerdict.setTextColor(Color.parseColor("#C5221F"))

            binding.tvChoiceMade.isVisible = true
            binding.tvChoiceMade.text = if (answer.wasTimeout) {
                getString(R.string.scenario_timeout_choice, answer.selectedOptionText)
            } else {
                answer.selectedOptionText
            }

            binding.tvFeedbackText.text = feedback?.text.orEmpty().ifBlank { getString(R.string.verdict_incorrect_desc) }
            binding.tvFeedbackNext.text = getString(R.string.verdict_incorrect_learn)
            binding.tvFeedbackCaption.text = getString(R.string.verdict_error_logged)

            binding.btnContinue.text = getString(
                if (answer.isLast) R.string.scenario_see_result else R.string.scenario_continue
            )
            binding.btnContinue.setOnClickListener {
                // Keep the same attempt alive; it is saved with every decision at scenario end.
                viewModel.continueAfterAnswer()
            }
        }
    }

    companion object {
        const val ARG_SCENARIO_ID = "scenarioId"
    }
}
