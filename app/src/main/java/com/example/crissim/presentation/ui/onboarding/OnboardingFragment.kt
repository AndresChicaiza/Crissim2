package com.example.crissim.presentation.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.crissim.R
import com.example.crissim.databinding.FragmentOnboardingBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

/**
 * Fragment de onboarding (HU-01).
 * Carrusel de 4 pantallas explicativas con indicador de progreso.
 * Se muestra solo la primera vez; se puede volver a ver desde Ajustes.
 */
class OnboardingFragment : Fragment() {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    private val pages = listOf(
        OnboardingPage(
            imageRes = R.drawable.ilc_logo,
            titleRes = R.string.onboarding_page1_title,
            descriptionRes = R.string.onboarding_page1_description
        ),
        OnboardingPage(
            imageRes = R.drawable.ilc_logo,
            titleRes = R.string.onboarding_page2_title,
            descriptionRes = R.string.onboarding_page2_description
        ),
        OnboardingPage(
            imageRes = R.drawable.ilc_logo,
            titleRes = R.string.onboarding_page3_title,
            descriptionRes = R.string.onboarding_page3_description
        ),
        OnboardingPage(
            imageRes = R.drawable.ilc_logo,
            titleRes = R.string.onboarding_page4_title,
            descriptionRes = R.string.onboarding_page4_description
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        
        // Si ya completó el onboarding, navegar directamente al siguiente destino
        val prefs = requireContext().getSharedPreferences("crisissim_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_completed", false)) {
            findNavController().navigate(R.id.welcomeFragment)
            return
        }
        
        setupViewPager()
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

    private fun setupViewPager() {
        val adapter = OnboardingAdapter(
            pages = pages,
            onSkipClick = { finishOnboarding() },
            onNextClick = { position ->
                binding.viewPagerOnboarding.setCurrentItem(position, true)
            },
            onFinishClick = { finishOnboarding() }
        )

        binding.viewPagerOnboarding.adapter = adapter
        binding.viewPagerOnboarding.offscreenPageLimit = pages.size

        // Conectar TabLayout (indicador de puntos) con ViewPager2
        TabLayoutMediator(binding.tabLayoutOnboarding, binding.viewPagerOnboarding) { tab, _ ->
            // Los tabs se crean automáticamente; solo necesitamos que existan
        }.attach()
    }

    private fun finishOnboarding() {
        // Marcar onboarding como completado
        markOnboardingCompleted()

        // Navegar a WelcomeFragment (selección de idioma)
        findNavController().navigate(R.id.welcomeFragment)
    }

    private fun markOnboardingCompleted() {
        requireContext().getSharedPreferences("crisissim_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("onboarding_completed", true)
            .apply()
    }

    companion object {
        /**
         * Verifica si el onboarding ya se completó.
         * Si no, navega al onboarding; si sí, navega al destino indicado.
         */
        fun navigateIfNeeded(fragment: Fragment, nextDestination: Int) {
            val prefs = fragment.requireContext().getSharedPreferences("crisissim_prefs", Context.MODE_PRIVATE)
            val completed = prefs.getBoolean("onboarding_completed", false)

            if (!completed) {
                fragment.findNavController().navigate(R.id.onboardingFragment)
            } else {
                fragment.findNavController().navigate(nextDestination)
            }
        }

        /**
         * Permite volver a ver el onboarding desde Ajustes.
         */
        fun restartOnboarding(fragment: Fragment) {
            val prefs = fragment.requireContext().getSharedPreferences("crisissim_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("onboarding_completed", false).apply()
            fragment.findNavController().navigate(R.id.onboardingFragment)
        }
    }
}