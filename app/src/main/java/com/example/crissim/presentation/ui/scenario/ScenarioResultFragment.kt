package com.example.crissim.presentation.ui.scenario

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.crissim.R
import com.example.crissim.databinding.FragmentScenarioResultBinding

class ScenarioResultFragment : Fragment() {

    private var _binding: FragmentScenarioResultBinding? = null
    private val binding get() = _binding!!

    private var scenarioId: String = "sis-01"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scenarioId = arguments?.getString(ScenarioFragment.ARG_SCENARIO_ID) ?: "sis-01"
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
                bundleOf(ScenarioFragment.ARG_SCENARIO_ID to scenarioId)
            )
        }
    }
}
