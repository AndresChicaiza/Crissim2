package com.example.crissim.presentation.ui.settings

import android.app.UiModeManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.fragment.findNavController
import com.example.crissim.CrisisSimApp
import com.example.crissim.R
import com.example.crissim.databinding.FragmentSettingsBinding
import com.example.crissim.domain.model.AttemptResult
import com.example.crissim.domain.model.DecisionRecord
import com.example.crissim.domain.model.EndType
import com.example.crissim.presentation.ui.onboarding.OnboardingFragment
import com.example.crissim.presentation.viewmodel.LevelsViewModel
import com.example.crissim.util.LocaleHelper
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.navOptions
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // ViewModel usando factory del contenedor de la app
    private val levelsViewModel: LevelsViewModel by viewModels {
        LevelsViewModel.Factory
    }

    // SharedPreferences para accesibilidad
    private val accessibilityPrefs: SharedPreferences by lazy {
        requireContext().getSharedPreferences("accessibility_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        setupLanguageSelector()
        setupAccessibilitySettings()
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

    private fun setupLanguageSelector() {
        // Cargar idioma actual y actualizar UI
        val currentLang = LocaleHelper.getLanguage(requireContext())
        updateLanguageUi(currentLang)

        binding.cardLangEs.setOnClickListener { setLanguage("es") }
        binding.cardLangEn.setOnClickListener { setLanguage("en") }
    }

    private fun setLanguage(language: String) {
        LocaleHelper.setLanguage(requireContext(), language)
        updateLanguageUi(language)
        // Recrear la activity para aplicar el cambio en toda la UI
        requireActivity().recreate()
    }

    private fun updateLanguageUi(language: String) {
        val primaryColor = requireContext().getColor(R.color.primary)
        val secondaryColor = requireContext().getColor(R.color.text_secondary)
        val surfaceColor = requireContext().getColor(R.color.surface_light)

        if (language == "es") {
            binding.cardLangEs.setCardBackgroundColor(primaryColor)
            binding.tvLangEs.setTextColor(requireContext().getColor(R.color.on_primary_container))
            binding.cardLangEn.setCardBackgroundColor(surfaceColor)
            binding.tvLangEn.setTextColor(secondaryColor)
            binding.tvLanguageStatus.text = getString(R.string.welcome_lang_es) + " seleccionado. " + getString(R.string.welcome_lang_en) + " disponible."
        } else {
            binding.cardLangEn.setCardBackgroundColor(primaryColor)
            binding.tvLangEn.setTextColor(requireContext().getColor(R.color.on_primary_container))
            binding.cardLangEs.setCardBackgroundColor(surfaceColor)
            binding.tvLangEs.setTextColor(secondaryColor)
            binding.tvLanguageStatus.text = getString(R.string.welcome_lang_en) + " selected. " + getString(R.string.welcome_lang_es) + " available."
        }
    }

    private fun setupAccessibilitySettings() {
        // Cargar preferencias guardadas
        val soundEnabled = accessibilityPrefs.getBoolean("sound_enabled", true)
        val vibrationEnabled = accessibilityPrefs.getBoolean("vibration_enabled", true)
        val highContrastEnabled = accessibilityPrefs.getBoolean("high_contrast_enabled", false)
        val largeFontEnabled = accessibilityPrefs.getBoolean("large_font_enabled", false)

        // Configurar switches
        binding.switchSound.isChecked = soundEnabled
        binding.switchVibration.isChecked = vibrationEnabled
        binding.switchHighContrast.isChecked = highContrastEnabled
        binding.switchLargeFont.isChecked = largeFontEnabled

        // Aplicar alto contraste al inicio si estaba activado
        if (highContrastEnabled) {
            applyHighContrastFilter(true)
        }

        // Listeners para guardar cambios
        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            accessibilityPrefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            accessibilityPrefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        binding.switchHighContrast.setOnCheckedChangeListener { _, isChecked ->
            accessibilityPrefs.edit().putBoolean("high_contrast_enabled", isChecked).apply()
            applyHighContrastFilter(isChecked)
        }

        binding.switchLargeFont.setOnCheckedChangeListener { _, isChecked ->
            accessibilityPrefs.edit().putBoolean("large_font_enabled", isChecked).apply()
            applyLargeFont(isChecked)
        }
    }

    private fun applyHighContrastFilter(enabled: Boolean) {
        if (enabled) {
            // Usar modo noche del sistema para alto contraste (API 29+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_YES
            }
            // También aplicar modo noche de AppCompat para compatibilidad
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
                uiModeManager.nightMode = UiModeManager.MODE_NIGHT_NO
            }
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        // NO recreate() - el cambio de modo UI se aplica inmediatamente
    }

    private fun applyHighContrast(enabled: Boolean) {
        accessibilityPrefs.edit().putBoolean("high_contrast_enabled", enabled).apply()
        applyHighContrastFilter(enabled)
    }

    private fun applyLargeFont(enabled: Boolean) {
        val scale = if (enabled) 1.3f else 1.0f
        val config = requireContext().resources.configuration
        config.fontScale = scale
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)
    }

    private fun setupButtons() {
        binding.btnClearData.setOnClickListener {
            Snackbar.make(binding.root, R.string.settings_privacy_title, Snackbar.LENGTH_SHORT).show()
        }

        binding.cardMeetingPoint.setOnClickListener {
            findNavController().navigate(R.id.meetingPointFragment)
        }

        binding.cardOnboarding.setOnClickListener {
            OnboardingFragment.restartOnboarding(this)
        }

        binding.btnSignOut.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(
                R.id.authFragment,
                null,
                navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
            )
        }

        // Debug: Crear datos de prueba para certificados
        binding.btnDebugCreateTestData.setOnClickListener {
            createTestDataForCertificates()
        }
    }

    private fun createTestDataForCertificates() {
        val container = CrisisSimApp.instance.container
        val userRepository = container.userRepository
        val progressRepository = container.progressRepository
        val scenarioRepository = container.scenarioRepository

        requireActivity().lifecycleScope.launch {
            val user = userRepository.getActiveUser()
            if (user == null) {
                Snackbar.make(binding.root, "No hay usuario activo. Crea un perfil primero.", Snackbar.LENGTH_LONG).show()
                return@launch
            }

            val userId = user.id
            val scenariosToPass = listOf(
                "sis-01", "sis-02", "fire-basic-01", "inc-01", "hum-01", "hum-02"
            )

            for (scenarioId in scenariosToPass) {
                val scenario = scenarioRepository.getScenarioById(scenarioId)
                if (scenario != null) {
                    // Crear DecisionRecord para cada nodo con opciones (simular decisiones correctas)
                    val questionNodes = scenario.nodes.filter { it.options.isNotEmpty() }
                    val decisions = questionNodes.map { node ->
                        // Tomar la primera opción correcta, o la primera opción si no hay correcta marcada
                        val correctOption = node.options.firstOrNull { it.feedback?.isCorrect == true }
                            ?: node.options.first()
                        DecisionRecord(
                            nodeId = node.id,
                            optionId = correctOption.id,
                            timeToDecideMs = 5000,
                            wasTimeout = false,
                            scoreChange = correctOption.scoreChange,
                            isCorrect = correctOption.feedback?.isCorrect == true
                        )
                    }

                    // Crear un AttemptResult simulado con puntuación aprobatoria
                    val attemptResult = AttemptResult(
                        scenarioId = scenarioId,
                        userId = userId,
                        score = scenario.calculateMaxScore(),
                        maxScore = scenario.calculateMaxScore(),
                        isPassed = true,
                        decisions = decisions,
                        startedAt = System.currentTimeMillis() - 10000,
                        completedAt = System.currentTimeMillis(),
                        endNodeId = "node_end_success",
                        endType = EndType.SUCCESS
                    )
                    progressRepository.recordAttemptResult(attemptResult)
                }
            }

            // Forzar actualización del ViewModel
            levelsViewModel.refresh()

            Snackbar.make(binding.root, "✅ Datos de prueba creados. Nivel Básico aprobado.", Snackbar.LENGTH_LONG).show()
        }
    }
}