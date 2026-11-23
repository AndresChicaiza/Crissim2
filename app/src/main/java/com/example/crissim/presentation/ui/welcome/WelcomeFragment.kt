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
import com.example.crissim.presentation.ui.onboarding.OnboardingFragment
import com.example.crissim.util.LocaleHelper
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    private val binding get() = _binding!!

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
        checkOnboardingAndUser()
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

    private fun checkOnboardingAndUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Primero verificar si ya completó el onboarding
            OnboardingFragment.navigateIfNeeded(this@WelcomeFragment, R.id.authFragment)
            
            // Si ya completó onboarding, verificar si tiene usuario
            val prefs = requireContext().getSharedPreferences("crisissim_prefs", android.content.Context.MODE_PRIVATE)
            val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)
            
            if (onboardingCompleted) {
                val userRepo = CrisisSimApp.instance.container.userRepository
                if (userRepo.hasUser()) {
                    findNavController().navigate(R.id.homeFragment)
                }
            }
        }
    }

    private fun setupLanguageSelection() {
        // Cargar idioma guardado
        val currentLang = LocaleHelper.getLanguage(requireContext())
        updateLanguageUi(currentLang)

        binding.cardLangEs.setOnClickListener {
            setLanguage("es")
        }
        binding.cardLangEn.setOnClickListener {
            setLanguage("en")
        }
    }

    private fun setLanguage(language: String) {
        LocaleHelper.setLanguage(requireContext(), language)
        updateLanguageUi(language)
        // Recrear la activity para aplicar el cambio en toda la UI
        requireActivity().recreate()
    }

    private fun updateLanguageUi(language: String) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val defaultBorderColor = ContextCompat.getColor(requireContext(), R.color.primary_border)

        if (language == "es") {
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
            findNavController().navigate(R.id.authFragment)
        }
    }
}
