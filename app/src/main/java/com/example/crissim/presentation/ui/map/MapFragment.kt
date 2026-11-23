package com.example.crissim.presentation.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.crissim.databinding.FragmentMapBinding

/**
 * Pantalla de Plano Digital de Evacuación y Punto de Encuentro (HU-12, HU-14).
 * Funciona 100% offline sin necesidad de GPS.
 */
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
    }

    private fun setupActions() {
        binding.btnMeetingPoint.setOnClickListener {
            val location = binding.etLocation.text?.toString().orEmpty().trim()
            showEvacuationRoute(location)
        }
    }

    private fun showEvacuationRoute(location: String) {
        val placeName = if (location.isNotEmpty()) location else "tu ubicación actual"
        binding.tvRouteTitle.text = "Ruta hacia el Punto de Encuentro desde $placeName:"
        binding.cardRouteResult.isVisible = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
