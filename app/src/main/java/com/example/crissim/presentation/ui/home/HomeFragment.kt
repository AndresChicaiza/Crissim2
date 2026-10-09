package com.example.crissim.presentation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.crissim.R
import com.example.crissim.databinding.FragmentHomeBinding
import com.example.crissim.databinding.ItemLevelProgressBinding
import com.example.crissim.domain.model.Level
import com.example.crissim.presentation.viewmodel.HomeUiState
import com.example.crissim.presentation.viewmodel.HomeViewModel
import com.example.crissim.presentation.viewmodel.LevelUi
import kotlinx.coroutines.launch

/**
 * Pantalla Inicio: pide el nombre la primera vez y muestra el progreso por niveles.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels { HomeViewModel.Factory }

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
        setupNameForm()
        observeState()
    }

    override fun onStart() {
        super.onStart()
        // El progreso pudo cambiar al jugar un escenario.
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

    private fun setupNameForm() {
        binding.btnSaveName.setOnClickListener { submitName() }
        binding.etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitName()
                true
            } else {
                false
            }
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

    private fun submitName() {
        val name = binding.etName.text?.toString().orEmpty().trim()
        if (name.isEmpty()) {
            binding.tilName.error = getString(R.string.home_name_error)
            return
        }
        binding.tilName.error = null
        viewModel.saveName(name)
    }

    // ─── Dibujo de la pantalla ───

    private fun render(state: HomeUiState) {
        binding.progressLoading.isVisible = state is HomeUiState.Loading
        binding.cardName.isVisible = state is HomeUiState.NeedsName
        binding.groupReady.isVisible = state is HomeUiState.Ready

        when (state) {
            HomeUiState.Loading -> Unit
            HomeUiState.NeedsName -> binding.tvTitle.setText(R.string.home_title_welcome)
            is HomeUiState.Ready -> renderReady(state)
        }
    }

    private fun renderReady(state: HomeUiState.Ready) {
        binding.tvTitle.text = getString(R.string.home_title_greeting, state.userName)
        binding.tvScenarios.text =
            getString(R.string.home_scenarios_available, state.scenarioCount)

        binding.levelsContainer.removeAllViews()
        state.levels.forEach { addLevelRow(it) }
    }

    private fun addLevelRow(item: LevelUi) {
        val row = ItemLevelProgressBinding.inflate(layoutInflater, binding.levelsContainer, false)
        row.tvLevelName.setText(levelNameRes(item.level))
        row.tvLevelDesc.setText(levelDescRes(item.level))
        row.tvLevelStatus.text = statusText(item)
        row.progressLevel.setProgressCompat(item.percent, false)
        row.root.alpha = if (item.isUnlocked) 1f else 0.5f
        binding.levelsContainer.addView(row.root)
    }

    private fun statusText(item: LevelUi): String = when {
        !item.isUnlocked -> getString(R.string.level_status_locked)
        item.isCompleted ->
            getString(R.string.level_status_completed, item.bestScore, item.maxScore)
        item.attempts > 0 ->
            getString(R.string.level_status_in_progress, item.bestScore, item.maxScore)
        else -> getString(R.string.level_status_available)
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
}