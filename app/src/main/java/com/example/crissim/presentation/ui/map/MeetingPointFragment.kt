package com.example.crissim.presentation.ui.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.crissim.R
import com.example.crissim.databinding.FragmentMeetingPointBinding

class MeetingPointFragment : Fragment() {

    private var _binding: FragmentMeetingPointBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetingPointBinding.inflate(inflater, container, false)
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
        binding.btnReturnHome.setOnClickListener {
            val navController = findNavController()
            if (!navController.popBackStack(R.id.homeFragment, false)) {
                navController.navigate(
                    R.id.homeFragment,
                    null,
                    navOptions {
                        popUpTo(navController.graph.id) { inclusive = true }
                        launchSingleTop = true
                    }
                )
            }
        }
    }
}
