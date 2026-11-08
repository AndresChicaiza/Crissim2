package com.example.crissim.presentation.ui.levels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
import com.example.crissim.databinding.FragmentLevelsBinding
import com.example.crissim.databinding.ItemScenarioCardBinding
import com.example.crissim.domain.model.ScenarioType
import com.example.crissim.presentation.ui.scenario.ScenarioFragment
import com.example.crissim.presentation.viewmodel.LevelsUiState
import com.example.crissim.presentation.viewmodel.LevelsViewModel
import com.example.crissim.presentation.viewmodel.ScenarioItemUi
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LevelsFragment : Fragment() {

    private var _binding: FragmentLevelsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LevelsViewModel by viewModels { LevelsViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        setupMeetingPoint()
        observeState()
    }

    override fun onStart() {
        super.onStart()
        viewModel.refresh()
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

    private fun setupMeetingPoint() {
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

    private fun render(state: LevelsUiState) {
        binding.progressLoading.isVisible = state is LevelsUiState.Loading

        if (state is LevelsUiState.Ready) {
            val allScenarios = state.sections.flatMap { it.scenarios }
            val sismos = allScenarios.filter { it.type == ScenarioType.EARTHQUAKE }
            val incendios = allScenarios.filter { it.type == ScenarioType.FIRE }
            val humo = allScenarios.filter { it.type == ScenarioType.SMOKE }

            renderScenarios(binding.containerHumo, humo)
            renderScenarios(binding.containerIncendios, incendios)
            renderScenarios(binding.containerSismos, sismos)
        }
    }

    private fun renderScenarios(container: ViewGroup, items: List<ScenarioItemUi>) {
        container.removeAllViews()
        val orderedItems = items.sortedBy { progressionOrder(it.id) }
        orderedItems.forEachIndexed { index, item ->
            val cardBinding = ItemScenarioCardBinding.inflate(layoutInflater, container, false)

            val code = item.id.uppercase()
            val isUnlocked = index == 0 || orderedItems.take(index).all { it.isPassed }
            val statusText = when {
                item.isPassed -> getString(R.string.level_status_passed)
                item.attemptsUsed > 0 && isUnlocked -> getString(R.string.level_status_failed)
                isUnlocked -> getString(R.string.level_status_available)
                else -> getString(R.string.level_status_locked)
            }

            cardBinding.tvScenarioCodeStatus.text = "$code · $statusText"
            cardBinding.tvScenarioTitle.text = item.title
            cardBinding.tvScenarioMeta.text = "${levelLabel(item)} · ${estimatedTime(item)}"

            if (isUnlocked) {
                cardBinding.ivScenarioAction.setImageResource(R.drawable.ic_chevron_right)
                cardBinding.ivScenarioAction.setColorFilter(ContextCompat.getColor(requireContext(), R.color.primary))
                cardBinding.cardScenario.setOnClickListener {
                    findNavController().navigate(
                        R.id.scenarioContextFragment,
                        bundleOf(ScenarioFragment.ARG_SCENARIO_ID to item.id)
                    )
                }
            } else {
                cardBinding.ivScenarioAction.setImageResource(R.drawable.ic_lock)
                cardBinding.ivScenarioAction.setColorFilter(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                cardBinding.cardScenario.setOnClickListener {
                    Snackbar.make(binding.root, R.string.levels_locked_hint, Snackbar.LENGTH_SHORT).show()
                }
            }

            container.addView(cardBinding.root)
        }
    }

    private fun levelLabel(item: ScenarioItemUi): String {
        return "Nivel ${progressionOrder(item.id)}"
    }

    private fun estimatedTime(item: ScenarioItemUi): String {
        val seconds = item.questionCount * 30
        val minutes = (seconds + 59) / 60
        return "${item.questionCount} preguntas · $minutes min"
    }

    private fun progressionOrder(id: String): Int {
        val numericSuffix = id.substringAfterLast('-').toIntOrNull()
        return when {
            id.startsWith("sis-") -> numericSuffix ?: Int.MAX_VALUE
            id.startsWith("hum-") -> numericSuffix ?: Int.MAX_VALUE
            id == "fire-basic-01" -> 1
            id.startsWith("inc-") -> (numericSuffix ?: Int.MAX_VALUE) + 1
            else -> Int.MAX_VALUE
        }
    }
}
