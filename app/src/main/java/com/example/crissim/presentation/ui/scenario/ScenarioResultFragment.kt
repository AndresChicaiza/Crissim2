package com.example.crissim.presentation.ui.scenario

import android.os.Bundle
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.example.crissim.R
import com.example.crissim.databinding.FragmentScenarioResultBinding

class ScenarioResultFragment : Fragment() {

    private var _binding: FragmentScenarioResultBinding? = null
    private val binding get() = _binding!!

    private var scenarioId: String = "sis-01"
    private var scenarioTitle: String = ""
    private var isPassed: Boolean = false
    private var correctCount: Int = 0
    private var totalCount: Int = 0
    private var percent: Int = 0
    private var score: Int = 0
    private var maxScore: Int = 0
    private var durationSeconds: Long = 0L
    private var saved: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scenarioId = arguments?.getString(ScenarioFragment.ARG_SCENARIO_ID) ?: "sis-01"
        scenarioTitle = arguments?.getString(ARG_SCENARIO_TITLE).orEmpty()
        isPassed = arguments?.getBoolean(ARG_RESULT_PASSED) ?: false
        correctCount = arguments?.getInt(ARG_RESULT_CORRECT_COUNT) ?: 0
        totalCount = arguments?.getInt(ARG_RESULT_TOTAL_COUNT) ?: 0
        percent = arguments?.getInt(ARG_RESULT_PERCENT) ?: 0
        score = arguments?.getInt(ARG_RESULT_SCORE) ?: 0
        maxScore = arguments?.getInt(ARG_RESULT_MAX_SCORE) ?: 0
        durationSeconds = arguments?.getLong(ARG_RESULT_DURATION) ?: 0L
        saved = arguments?.getBoolean(ARG_RESULT_SAVED) ?: true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScenarioResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        renderResult()
        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyStatusBarInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = top)
            insets
        }
    }

    private fun setupButtons() {
        binding.btnReflect.setOnClickListener {
            findNavController().navigate(
                R.id.scenarioReflectionFragment,
                bundleOf(ScenarioFragment.ARG_SCENARIO_ID to scenarioId)
            )
        }

        binding.btnRetryScenario.setOnClickListener {
            findNavController().navigate(
                R.id.scenarioFragment,
                bundleOf(ScenarioFragment.ARG_SCENARIO_ID to scenarioId),
                androidx.navigation.navOptions {
                    popUpTo(R.id.scenarioResultFragment) { inclusive = true }
                    launchSingleTop = true
                }
            )
        }

        binding.btnReturnDashboard.setOnClickListener {
            val navController = findNavController()
            if (!navController.popBackStack(R.id.homeFragment, false)) {
                navController.navigate(
                    R.id.homeFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                )
            }
        }

        binding.btnRetryScenario.text = getString(R.string.result_retry_button_format, scenarioId.uppercase())
    }

    private fun renderResult() {
        val scenarioLabel = scenarioTitle.ifBlank { scenarioId.uppercase() }
        binding.tvHeaderSubtitle.text = getString(
            R.string.result_header_subtitle_format,
            scenarioId.uppercase(),
            scenarioLabel
        )
        binding.tvResultBadge.text = getString(
            if (isPassed) R.string.result_pass_badge else R.string.result_fail_badge
        )
        binding.tvResultBadge.setBackgroundResource(
            if (isPassed) R.drawable.bg_pill_correct else R.drawable.bg_pill_incorrect
        )
        binding.tvResultBadge.setTextColor(
            if (isPassed) Color.parseColor("#176343") else Color.parseColor("#C5221F")
        )
        binding.tvResultTitle.text = getString(
            if (isPassed) R.string.result_pass_title else R.string.result_fail_title
        )
        binding.tvResultTitle.setTextColor(
            if (isPassed) ContextCompat.getColor(requireContext(), R.color.primary)
            else Color.parseColor("#C5221F")
        )
        binding.tvUserMeta.text = if (saved) {
            getString(R.string.result_score_meta, score, maxScore, percent)
        } else {
            getString(R.string.result_score_meta_not_saved, score, maxScore, percent)
        }

        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        binding.tvStatTime.text = getString(R.string.result_time_format, minutes, seconds)
        binding.tvStatErrors.text = (totalCount - correctCount).coerceAtLeast(0).toString()
        binding.tvStatAccuracy.text = "$percent%"
        binding.tvCorrectCount.text = getString(
            R.string.result_correct_count,
            correctCount,
            totalCount
        )

        binding.tvResultEvidenceTitle.text = getString(R.string.result_attempt_summary)
        binding.tvResultEvidence1.text = getString(R.string.result_correct_answers, correctCount)
        binding.tvResultEvidence2.text = getString(
            R.string.result_incorrect_answers,
            (totalCount - correctCount).coerceAtLeast(0)
        )
        binding.cardResultImprovement.isVisible = !isPassed
        if (!isPassed) {
            binding.tvResultImprovementTitle.text = getString(R.string.result_failed_card_title)
            binding.tvResultImprovementDesc.text = getString(
                R.string.result_failed_card_desc,
                totalCount,
                correctCount
            )
        }
    }

    companion object {
        const val ARG_SCENARIO_TITLE = "scenarioTitle"
        const val ARG_RESULT_PASSED = "resultPassed"
        const val ARG_RESULT_CORRECT_COUNT = "resultCorrectCount"
        const val ARG_RESULT_TOTAL_COUNT = "resultTotalCount"
        const val ARG_RESULT_PERCENT = "resultPercent"
        const val ARG_RESULT_SCORE = "resultScore"
        const val ARG_RESULT_MAX_SCORE = "resultMaxScore"
        const val ARG_RESULT_DURATION = "resultDurationSeconds"
        const val ARG_RESULT_SAVED = "resultSaved"
    }
}
