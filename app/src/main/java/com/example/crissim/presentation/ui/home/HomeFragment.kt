package com.example.crissim.presentation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.crissim.presentation.viewmodel.LevelUi
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

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
        setupButtons()
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

    private fun setupButtons() {
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
}
