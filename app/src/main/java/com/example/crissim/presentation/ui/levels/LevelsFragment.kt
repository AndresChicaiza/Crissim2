package com.example.crissim.presentation.ui.levels

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
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
import com.example.crissim.databinding.ItemLevelSectionBinding
import com.example.crissim.databinding.ItemScenarioBinding
import com.example.crissim.domain.model.Level
import com.example.crissim.domain.model.ScenarioType
import com.example.crissim.presentation.ui.scenario.ScenarioFragment
import com.example.crissim.presentation.viewmodel.LevelSectionUi
import com.example.crissim.presentation.viewmodel.LevelsUiState
import com.example.crissim.presentation.viewmodel.LevelsViewModel
import com.example.crissim.presentation.viewmodel.ScenarioItemUi
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Pantalla Niveles: escenarios agrupados por nivel, con desbloqueo progresivo.
 */
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
        binding.btnGoHome.setOnClickListener { goToHome() }
        observeState()
    }

    override fun onStart() {
        super.onStart()
        // El usuario o su progreso pueden haber cambiado desde otra pantalla.
        viewModel.refresh()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ─── Inicialización ───

    /** La app dibuja bajo la barra de estado: se añade su alto como margen superior. */
    private fun applyStatusBarInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = top)
            insets
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    // ─── Acciones ───

    /** Equivale a tocar la pestaña Inicio de la barra inferior. */
    private fun goToHome() {
        requireActivity()
            .findViewById<BottomNavigationView>(R.id.bottom_navigation)
            .selectedItemId = R.id.homeFragment
    }

    private fun onScenarioClicked(item: ScenarioItemUi, unlocked: Boolean) {
        if (!unlocked) {
            showMessage(R.string.levels_locked_hint)
            return
        }
        findNavController().navigate(
            R.id.scenarioFragment,
            bundleOf(ScenarioFragment.ARG_SCENARIO_ID to item.id)
        )
    }

    private fun showMessage(@StringRes messageRes: Int) {
        val bottomNav = requireActivity().findViewById<View>(R.id.bottom_navigation)
        Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT)
            .setAnchorView(bottomNav)
            .show()
    }

    // ─── Dibujo de la pantalla ───

    private fun render(state: LevelsUiState) {
        binding.progressLoading.isVisible = state is LevelsUiState.Loading
        binding.groupNoUser.isVisible = state is LevelsUiState.NeedsUser
        binding.sectionsContainer.isVisible = state is LevelsUiState.Ready

        if (state is LevelsUiState.Ready) {
            renderSections(state.sections)
        }
    }

    private fun renderSections(sections: List<LevelSectionUi>) {
        binding.sectionsContainer.removeAllViews()
        sections.forEach { addSection(it) }
    }

    private fun addSection(section: LevelSectionUi) {
        val sectionRow =
            ItemLevelSectionBinding.inflate(layoutInflater, binding.sectionsContainer, false)
        sectionRow.tvSectionName.setText(levelNameRes(section.level))
        sectionRow.tvSectionDesc.setText(levelDescRes(section.level))
        sectionRow.tvSectionStatus.setText(sectionStatusRes(section))
        sectionRow.tvEmpty.isVisible = section.scenarios.isEmpty()
        sectionRow.root.alpha = if (section.isUnlocked) 1f else 0.5f

        section.scenarios.forEach { addScenario(sectionRow, it, section.isUnlocked) }
        binding.sectionsContainer.addView(sectionRow.root)
    }

    private fun addScenario(
        sectionRow: ItemLevelSectionBinding,
        item: ScenarioItemUi,
        unlocked: Boolean
    ) {
        val card = ItemScenarioBinding.inflate(layoutInflater, sectionRow.scenariosContainer, false)
        card.tvScenarioType.setText(typeLabelRes(item.type))
        card.tvScenarioTitle.text = item.title
        card.tvScenarioDesc.text = item.description
        card.tvScenarioMeta.text = getString(
            R.string.scenario_meta,
            item.passingPercent,
            item.maxScore,
            item.maxAttempts
        )
        card.tvScenarioResult.text = resultText(item)
        card.root.setOnClickListener { onScenarioClicked(item, unlocked) }
        sectionRow.scenariosContainer.addView(card.root)
    }

    // ─── Textos ───

    private fun resultText(item: ScenarioItemUi): String = when {
        item.attemptsUsed == 0 -> getString(R.string.scenario_result_none)
        item.isPassed -> getString(
            R.string.scenario_result_passed,
            item.bestScore ?: 0,
            item.maxScore,
            item.attemptsUsed
        )
        else -> getString(
            R.string.scenario_result_progress,
            item.bestScore ?: 0,
            item.maxScore,
            item.attemptsUsed
        )
    }

    private fun sectionStatusRes(section: LevelSectionUi): Int = when {
        !section.isUnlocked -> R.string.level_status_locked
        section.isCompleted -> R.string.levels_section_completed
        else -> R.string.level_status_available
    }

    private fun levelNameRes(level: Level): Int = when (level) {
        Level.BASIC -> R.string.level_basic
        Level.INTERMEDIATE -> R.string.level_intermediate
        Level.ADVANCED -> R.string.level_advanced
    }

    private fun levelDescRes(level: Level): Int = when (level) {
        Level.BASIC -> R.string.level_basic_desc
        Level.INTERMEDIATE -> R.string.level_intermediate_desc
        Level.ADVANCED -> R.string.level_advanced_desc
    }

    private fun typeLabelRes(type: ScenarioType): Int = when (type) {
        ScenarioType.FIRE -> R.string.scenario_type_fire
        ScenarioType.EARTHQUAKE -> R.string.scenario_type_earthquake
    }
}