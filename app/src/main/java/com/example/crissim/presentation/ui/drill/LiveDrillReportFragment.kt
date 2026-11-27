package com.example.crissim.presentation.ui.drill

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.crissim.R
import com.example.crissim.databinding.FragmentLiveDrillReportBinding
import com.example.crissim.domain.model.LiveDrill
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LiveDrillReportFragment : Fragment() {

    private var _binding: FragmentLiveDrillReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveDrillReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        setupButtons()

        @Suppress("DEPRECATION")
        val report = arguments?.getSerializable(LiveDrillFragment.ARG_DRILL_REPORT) as? LiveDrill
        if (report != null) {
            renderReport(report)
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
        binding.btnReportReturnHome.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun renderReport(report: LiveDrill) {
        binding.tvReportUserMeta.text = "${report.userName} (${report.role.labelEs}) · Responsable ILC"

        val minutes = report.totalDurationSeconds / 60
        val seconds = report.totalDurationSeconds % 60
        val formattedDuration = String.format(Locale.getDefault(), "%02d:%02d min", minutes, seconds)
        binding.tvReportDuration.text = formattedDuration

        val isWithinTarget = report.totalDurationSeconds <= report.targetTimeSeconds
        val targetMinutes = report.targetTimeSeconds / 60
        val targetSeconds = report.targetTimeSeconds % 60
        val formattedTarget = String.format(Locale.getDefault(), "%02d:%02d min", targetMinutes, targetSeconds)

        if (isWithinTarget) {
            binding.tvReportBadgeStatus.text = "✅ Evacuación Cumplida"
            binding.tvReportTargetComparison.text = "¡Dentro de la meta ILC! ($formattedDuration vs Meta: $formattedTarget)"
            binding.cardReportStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.meeting_point_bg))
            binding.cardReportStatus.strokeColor = ContextCompat.getColor(requireContext(), R.color.meeting_point_border)
        } else {
            binding.tvReportBadgeStatus.text = "⚠️ Tiempo Fuera de Meta"
            binding.tvReportTargetComparison.text = "Superó la meta ILC ($formattedDuration vs Meta: $formattedTarget)"
            binding.cardReportStatus.setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary_container))
            binding.cardReportStatus.strokeColor = ContextCompat.getColor(requireContext(), R.color.primary_border)
        }

        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        binding.tvReportStartedAt.text = "• Hora de inicio: ${timeFormat.format(Date(report.startedAt))}"
        binding.tvReportCompletedAt.text = "• Hora de evacuación: ${timeFormat.format(Date(report.completedAt))}"
        binding.tvReportDrillId.text = "• Código de reporte: ${report.id}"

        if (report.incidents.isEmpty()) {
            binding.tvReportIncidentsText.text = "No se registraron novedades ni incidencias durante la evacuación."
        } else {
            val incidentsText = report.incidents.joinToString("\n\n") { incident ->
                val timeStr = timeFormat.format(Date(incident.timestamp))
                "• [$timeStr] ${incident.stepTitle}:\n  ${incident.note}"
            }
            binding.tvReportIncidentsText.text = incidentsText
        }
    }
}
