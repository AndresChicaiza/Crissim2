package com.example.crissim.presentation.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.crissim.CrisisSimApp
import com.example.crissim.R
import com.example.crissim.databinding.FragmentWelcomeBinding
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

    private var selectedLanguage = "ES"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        checkExistingUser()
        setupLanguageSelection()
        setupButtons()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyStatusBarInset() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updatePadding(top = top)
            insets
        }
    }

    private fun checkExistingUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            val userRepo = CrisisSimApp.instance.container.userRepository
            if (userRepo.hasUser()) {
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    private fun setupLanguageSelection() {
        binding.cardLangEs.setOnClickListener {
            selectedLanguage = "ES"
            updateLanguageUi()
        }
        binding.cardLangEn.setOnClickListener {
            selectedLanguage = "EN"
            updateLanguageUi()
        }
    }

    private fun updateLanguageUi() {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val defaultBorderColor = ContextCompat.getColor(requireContext(), R.color.primary_border)

        if (selectedLanguage == "ES") {
            binding.cardLangEs.strokeColor = primaryColor
            binding.cardLangEs.strokeWidth = 4
            binding.ivLangEsCheck.visibility = View.VISIBLE

            binding.cardLangEn.strokeColor = defaultBorderColor
            binding.cardLangEn.strokeWidth = 2
        } else {
            binding.cardLangEn.strokeColor = primaryColor
            binding.cardLangEn.strokeWidth = 4

            binding.cardLangEs.strokeColor = defaultBorderColor
            binding.cardLangEs.strokeWidth = 2
            binding.ivLangEsCheck.visibility = View.GONE
        }
    }

    private fun setupButtons() {
        binding.btnStart.setOnClickListener {
            findNavController().navigate(R.id.profileSetupFragment)
        }
    }
}
