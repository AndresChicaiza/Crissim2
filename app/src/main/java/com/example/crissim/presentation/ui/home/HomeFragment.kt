package com.example.crissim.presentation.ui.home

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
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
import com.example.crissim.R
import com.example.crissim.databinding.FragmentHomeBinding
import com.example.crissim.domain.model.Level
import com.example.crissim.presentation.ui.scenario.ScenarioFragment
import com.example.crissim.presentation.viewmodel.HomeUiState
import com.example.crissim.presentation.viewmodel.HomeViewModel
import com.example.crissim.presentation.viewmodel.MicroTrainingUiState
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

    private var currentMicroDialog: Dialog? = null
    private var currentDialogView: View? = null
    private var lastMicroState: MicroTrainingUiState? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        setupButtons()
        observeState()
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentMicroDialog?.dismiss()
        currentMicroDialog = null
        currentDialogView = null
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
        binding.cardMicroTrainingLauncher.setOnClickListener {
            showMicroTrainingDialog()
        }

        binding.btnOpenMicroDialog.setOnClickListener {
            showMicroTrainingDialog()
        }

        binding.cardLiveDrillLauncher.setOnClickListener {
            findNavController().navigate(R.id.liveDrillFragment)
        }

        binding.btnOpenLiveDrill.setOnClickListener {
            findNavController().navigate(R.id.liveDrillFragment)
        }

        binding.btnContinueScenario.setOnClickListener {
            findNavController().navigate(
                R.id.scenarioContextFragment,
                bundleOf(ScenarioFragment.ARG_SCENARIO_ID to "sis-01")
            )
        }

        binding.btnViewScenarios.setOnClickListener {
            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                .selectedItemId = R.id.levelsFragment
        }

        binding.btnEvacuationMap.setOnClickListener {
            findNavController().navigate(R.id.meetingPointFragment)
        }

        binding.cardMeetingPoint.setOnClickListener {
            findNavController().navigate(R.id.meetingPointFragment)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: HomeUiState) {
        binding.progressLoading.isVisible = state is HomeUiState.Loading

        when (state) {
            HomeUiState.Loading -> Unit
            HomeUiState.NeedsName -> {
                findNavController().navigate(R.id.welcomeFragment)
            }
            is HomeUiState.Ready -> renderReady(state)
        }
    }

    private fun renderReady(state: HomeUiState.Ready) {
        binding.tvUserRole.text = state.userRole.labelEs
        binding.tvGreeting.text = getString(R.string.dashboard_greeting, state.userName)

        lastMicroState = state.microTrainingUi
        binding.tvHomeMicroStreak.text = "Refuerzo espaciado SM-2 · " + getString(R.string.micro_streak_format, state.microTrainingUi.currentStreak)

        if (currentMicroDialog?.isShowing == true && currentDialogView != null) {
            updateDialogContent(currentDialogView!!, state.microTrainingUi, currentMicroDialog)
        }

        state.levels.forEach { item ->
            when (item.level) {
                Level.BASIC -> {
                    binding.tvBasicStatus.text = getString(R.string.level_basic_status, item.attempts, 4)
                }
                Level.INTERMEDIATE -> {
                    binding.tvIntermediateStatus.text = getString(R.string.level_intermediate_status, item.attempts, 2)
                }
                Level.ADVANCED -> {
                    binding.tvAdvancedStatus.text = getString(R.string.level_advanced_status, item.attempts, 2)
                }
            }
        }
    }

    private fun showMicroTrainingDialog() {
        if (currentMicroDialog?.isShowing == true) return

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_micro_training, null)
        currentDialogView = dialogView

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setOnDismissListener {
                currentMicroDialog = null
                currentDialogView = null
            }
            .create()

        currentMicroDialog = dialog
        lastMicroState?.let { updateDialogContent(dialogView, it, dialog) }
        dialog.show()
    }

    private fun updateDialogContent(root: View, ui: MicroTrainingUiState, dialog: Dialog? = null) {
        val tvStreak = root.findViewById<TextView>(R.id.tv_dialog_streak)
        val tvBadges = root.findViewById<TextView>(R.id.tv_dialog_badges)
        val tvQuestion = root.findViewById<TextView>(R.id.tv_dialog_question)
        val rgOptions = root.findViewById<RadioGroup>(R.id.rg_dialog_options)
        val btnSubmit = root.findViewById<View>(R.id.btn_dialog_submit)
        val btnClose = root.findViewById<View>(R.id.btn_dialog_close)
        val layoutFeedback = root.findViewById<View>(R.id.layout_dialog_feedback)
        val tvFeedbackTitle = root.findViewById<TextView>(R.id.tv_dialog_feedback_title)
        val tvExplanation = root.findViewById<TextView>(R.id.tv_dialog_explanation)

        tvStreak?.text = getString(R.string.micro_streak_format, ui.currentStreak)

        if (ui.earnedBadges.isEmpty()) {
            tvBadges?.text = getString(R.string.micro_badges_empty)
        } else {
            val names = ui.earnedBadges.joinToString(", ") { it.name }
            tvBadges?.text = getString(R.string.micro_badges_format, ui.earnedBadges.size, names)
        }

        btnClose?.setOnClickListener {
            dialog?.dismiss()
        }

        val micro = ui.nextMicroTraining?.microTraining
        if (micro == null) {
            tvQuestion?.text = "¡Has completado todos los micro-entrenamientos por hoy!"
            rgOptions?.removeAllViews()
            btnSubmit?.visibility = View.GONE
            return
        }

        btnSubmit?.visibility = View.VISIBLE
        tvQuestion?.text = micro.question

        if (rgOptions?.childCount != micro.options.size) {
            rgOptions?.removeAllViews()
            micro.options.forEach { option ->
                val radioButton = RadioButton(requireContext()).apply {
                    id = View.generateViewId()
                    text = option.text
                    layoutParams = RadioGroup.LayoutParams(
                        RadioGroup.LayoutParams.MATCH_PARENT,
                        RadioGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 8, 0, 8)
                    }
                    setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                    setPadding(16, 16, 16, 16)
                }
                rgOptions?.addView(radioButton)
            }
        }

        val isQuestionAnsweredCorrectly = (ui.lastResult != null && ui.lastResult.isCorrect)

        if (isQuestionAnsweredCorrectly) {
            btnSubmit?.isEnabled = false
            (btnSubmit as? android.widget.Button)?.text = getString(R.string.micro_btn_completed)
            for (i in 0 until (rgOptions?.childCount ?: 0)) {
                rgOptions?.getChildAt(i)?.isEnabled = false
            }
        } else {
            btnSubmit?.isEnabled = true
            (btnSubmit as? android.widget.Button)?.text = getString(R.string.micro_btn_submit)
            for (i in 0 until (rgOptions?.childCount ?: 0)) {
                rgOptions?.getChildAt(i)?.isEnabled = true
            }
        }

        val targetSnackBarView = activity?.findViewById<View>(android.R.id.content) ?: root

        btnSubmit?.setOnClickListener {
            val checkedId = rgOptions?.checkedRadioButtonId ?: -1
            if (checkedId == -1) {
                Snackbar.make(targetSnackBarView, "Selecciona una opción para responder", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val checkedIndex = rgOptions?.indexOfChild(rgOptions.findViewById(checkedId)) ?: -1
            if (checkedIndex != -1) {
                viewModel.submitMicroTrainingAnswer(checkedIndex)
            }
        }

        val result = ui.lastResult
        if (result != null) {
            layoutFeedback?.visibility = View.VISIBLE
            if (result.isCorrect) {
                tvFeedbackTitle?.text = getString(R.string.verdict_correct_title) + "\n" + result.selectedOption.feedback + "\n\n" + getString(R.string.micro_completed_hint)
                tvFeedbackTitle?.setTextColor(requireContext().getColor(R.color.color_correct))
            } else {
                tvFeedbackTitle?.text = getString(R.string.action_incorrect) + ": " + result.selectedOption.feedback
                tvFeedbackTitle?.setTextColor(requireContext().getColor(R.color.color_error))
            }
            tvExplanation?.text = result.explanation

            if (result.newlyEarnedBadges.isNotEmpty()) {
                val badgeNames = result.newlyEarnedBadges.joinToString(", ") { it.name }
                Snackbar.make(
                    targetSnackBarView,
                    getString(R.string.micro_badge_earned, badgeNames),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } else {
            layoutFeedback?.visibility = View.GONE
        }
    }
}
