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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.crissim.CrisisSimApp
import com.example.crissim.R
import com.example.crissim.databinding.FragmentScenarioContextBinding
import com.example.crissim.domain.model.Level
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScenarioContextFragment : Fragment() {

    private var _binding: FragmentScenarioContextBinding? = null
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
        _binding = FragmentScenarioContextBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        loadScenarioDetails()
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

    private fun loadScenarioDetails() {
        viewLifecycleOwner.lifecycleScope.launch {
            val scenarioRepo = CrisisSimApp.instance.container.scenarioRepository
            val scenario = withContext(Dispatchers.IO) { scenarioRepo.getScenarioById(scenarioId) }
            if (scenario != null) {
                binding.tvHeaderTitle.text = scenario.title.get()
                binding.tvHeaderSubtitle.text = "${scenario.id.uppercase()} · ${levelName(scenario.level)} · 2 min estimados"
                binding.tvTitle.text = scenario.title.get()
                val firstNode = scenario.nodes.firstOrNull()
                if (firstNode != null) {
                    binding.tvNarrative.text = firstNode.narrative.get()
                }
            }
        }
    }

    private fun levelName(level: Level): String = when (level) {
        Level.BASIC -> "Básico"
        Level.INTERMEDIATE -> "Intermedio"
        Level.ADVANCED -> "Avanzado"
    }

    private fun setupButtons() {
        binding.btnStartSimulation.setOnClickListener {
            findNavController().navigate(
                R.id.scenarioFragment,
                bundleOf(ScenarioFragment.ARG_SCENARIO_ID to scenarioId)
            )
        }

        binding.cardMeetingPoint.setOnClickListener {
            findNavController().navigate(R.id.meetingPointFragment)
        }
    }
}
