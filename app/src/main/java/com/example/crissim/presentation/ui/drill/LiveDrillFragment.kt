package com.example.crissim.presentation.ui.drill

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.example.crissim.databinding.FragmentLiveDrillBinding
import com.example.crissim.domain.model.LiveDrill
import com.example.crissim.presentation.viewmodel.LiveDrillUiState
import com.example.crissim.presentation.viewmodel.LiveDrillViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LiveDrillFragment : Fragment() {

    private var _binding: FragmentLiveDrillBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LiveDrillViewModel by viewModels { LiveDrillViewModel.Factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveDrillBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        setupButtons()
        observeState()

        // Iniciar simulacro si es la primera vez que se ingresa a la vista
        if (viewModel.state.value is LiveDrillUiState.Idle) {
            viewModel.startLiveDrill()
        }
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
        binding.btnToggleAlarm.setOnClickListener {
            viewModel.toggleMuteAlarm()
        }

        binding.btnPrevStep.setOnClickListener {
            viewModel.previousStep()
        }

        binding.btnNextStep.setOnClickListener {
            viewModel.nextStep()
        }

        binding.btnRecordIncident.setOnClickListener {
            showRecordIncidentDialog()
        }

        binding.btnViewMapDrill.setOnClickListener {
            findNavController().navigate(R.id.meetingPointFragment)
        }

        binding.btnFinishDrill.setOnClickListener {
            viewModel.finishDrillNow()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: LiveDrillUiState) {
        when (state) {
            LiveDrillUiState.Idle -> Unit
            is LiveDrillUiState.Active -> renderActive(state)
            is LiveDrillUiState.Completed -> navigateToReport(state.report)
        }
    }

    private fun renderActive(active: LiveDrillUiState.Active) {
        binding.tvDrillUserRole.text = "Guía en tiempo real · Rol: ${active.drill.role.labelEs}"

        val minutes = active.elapsedSeconds / 60
        val seconds = active.elapsedSeconds % 60
        binding.tvDrillTimer.text = String.format("%02d:%02d", minutes, seconds)

        if (active.isMuted) {
            binding.tvAlarmStatus.text = "🔕 Sirena silenciada (Guía activa)"
            binding.btnToggleAlarm.text = "Activar Sonido"
        } else {
            binding.tvAlarmStatus.text = "🔔 Alarma de emergencia activa"
            binding.btnToggleAlarm.text = "Silenciar"
        }

        val step = active.steps.getOrNull(active.currentStepIndex)
        if (step != null) {
            binding.tvStepIndicator.text = "Paso ${step.stepNumber} de ${active.steps.size}"
            binding.tvStepTitle.text = step.title
            binding.tvStepDescription.text = step.description
            binding.tvRoleAction.text = step.roleAction
        }

        binding.btnPrevStep.isEnabled = active.currentStepIndex > 0
        binding.btnNextStep.isEnabled = active.currentStepIndex < active.steps.size - 1

        binding.tvIncidentsCount.text = "📝 Incidencias registradas: ${active.incidents.size}"
    }

    private fun showRecordIncidentDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Escribe la observación o incidencia..."
            setPadding(32, 32, 32, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("📝 Registrar Incidencia / Observación")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isNotEmpty()) {
                    viewModel.recordIncident(note)
                    Snackbar.make(binding.root, "Incidencia registrada", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun navigateToReport(report: LiveDrill) {
        if (findNavController().currentDestination?.id == R.id.liveDrillFragment) {
            findNavController().navigate(
                R.id.liveDrillReportFragment,
                bundleOf(ARG_DRILL_REPORT to report)
            )
        }
    }

    companion object {
        const val ARG_DRILL_REPORT = "arg_drill_report"
    }
}
