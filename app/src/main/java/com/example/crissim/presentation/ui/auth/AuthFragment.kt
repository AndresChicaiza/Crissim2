package com.example.crissim.presentation.ui.auth

import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.crissim.R
import com.example.crissim.databinding.FragmentAuthBinding
import com.example.crissim.domain.model.UserRole
import com.example.crissim.domain.security.FieldError
import com.example.crissim.domain.security.LocalAuthResult
import com.example.crissim.presentation.viewmodel.AuthUiState
import com.example.crissim.presentation.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Acceso híbrido: "Con código" (local, sin internet) o "Con cuenta" (Firebase, con internet).
 * El modo local es el principal; la cuenta es opcional para sincronizar entre dispositivos.
 */
class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }

    private var useAccount = false
    private var registeringLocal = false
    private var registeringAccount = false
    private var selectedRole = UserRole.STUDENT
    private var countdownJob: Job? = null
    private var loading = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRoleDropdown()
        setupActions()
        renderMode()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::renderState)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val code = viewModel.localProfileCode()
            if (code != null) binding.etCode.setText(code) else registeringLocal = true
            renderMode()
        }
        viewModel.restoreSession()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownJob?.cancel()
        _binding = null
    }

    private fun setupRoleDropdown() {
        val roles = UserRole.entries
        binding.actvRole.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, roles.map { it.labelEs })
        )
        binding.actvRole.setText(selectedRole.labelEs, false)
        binding.actvRole.setOnItemClickListener { _, _, position, _ -> selectedRole = roles[position] }
    }

    private fun setupActions() {
        binding.toggleMode.addOnButtonCheckedListener { _, id, checked ->
            if (checked) {
                useAccount = id == R.id.btn_mode_account
                renderMode()
            }
        }

        // Local (sin internet)
        binding.btnLocalToggle.setOnClickListener {
            registeringLocal = !registeringLocal
            clearLocalErrors()
            renderMode()
        }
        binding.btnLocalSubmit.setOnClickListener {
            clearLocalErrors()
            val code = binding.etCode.text?.toString().orEmpty()
            val pin = binding.etPin.text?.toString().orEmpty()
            if (registeringLocal) {
                viewModel.registerLocal(binding.etLocalName.text?.toString().orEmpty(), code, pin, selectedRole)
            } else {
                viewModel.loginLocal(code, pin)
            }
        }

        // Cuenta Firebase (con internet)
        binding.btnAuthToggle.setOnClickListener {
            registeringAccount = !registeringAccount
            renderMode()
        }
        binding.btnAuthSubmit.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            if (registeringAccount) {
                viewModel.register(binding.etName.text?.toString().orEmpty(), email, password)
            } else {
                viewModel.signIn(email, password)
            }
        }
        binding.btnForgotPassword.setOnClickListener {
            viewModel.sendPasswordReset(binding.etEmail.text?.toString().orEmpty())
        }

        binding.btnOffline.setOnClickListener {
            findNavController().navigate(
                R.id.welcomeFragment,
                null,
                navOptions { popUpTo(R.id.authFragment) { inclusive = true } }
            )
        }
    }

    private fun renderMode() {
        binding.toggleMode.check(if (useAccount) R.id.btn_mode_account else R.id.btn_mode_local)
        binding.groupLocal.isVisible = !useAccount
        binding.groupAccount.isVisible = useAccount

        if (useAccount) {
            binding.tvAuthTitle.setText(if (registeringAccount) R.string.auth_title_register else R.string.auth_title_login)
            binding.tvAuthSubtitle.setText(R.string.auth_subtitle)
            binding.tilName.isVisible = registeringAccount
            binding.btnForgotPassword.isVisible = !registeringAccount
            binding.btnAuthSubmit.setText(if (registeringAccount) R.string.auth_register_button else R.string.auth_login_button)
            binding.btnAuthToggle.setText(if (registeringAccount) R.string.auth_switch_login else R.string.auth_switch_register)
        } else {
            binding.tvAuthTitle.setText(
                if (registeringLocal) R.string.auth_local_title_register else R.string.auth_local_title_login
            )
            binding.tvAuthSubtitle.setText(R.string.auth_local_subtitle)
            binding.tilLocalName.isVisible = registeringLocal
            binding.tilRole.isVisible = registeringLocal
            binding.btnLocalSubmit.setText(
                if (registeringLocal) R.string.auth_local_register_button else R.string.auth_local_login_button
            )
            binding.btnLocalToggle.setText(
                if (registeringLocal) R.string.auth_local_switch_login else R.string.auth_local_switch_register
            )
        }
    }

    private fun renderState(state: AuthUiState) {
        loading = state is AuthUiState.Loading
        binding.progressAuth.isVisible = loading
        binding.btnAuthSubmit.isEnabled = !loading
        binding.btnAuthToggle.isEnabled = !loading
        binding.btnLocalToggle.isEnabled = !loading
        updateLocalSubmitEnabled()
        when (state) {
            AuthUiState.Idle, AuthUiState.Loading -> Unit
            is AuthUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.consumeMessage()
            }
            is AuthUiState.Message -> {
                Snackbar.make(binding.root, state.text, Snackbar.LENGTH_LONG).show()
                viewModel.consumeMessage()
            }
            is AuthUiState.LocalFailure -> renderLocalFailure(state.result)
            is AuthUiState.SignedIn -> {
                if (findNavController().currentDestination?.id == R.id.authFragment) {
                    findNavController().navigate(
                        R.id.homeFragment,
                        null,
                        navOptions { popUpTo(R.id.authFragment) { inclusive = true } }
                    )
                }
                state.warning?.let { warning ->
                    Snackbar.make(requireActivity().findViewById(android.R.id.content), warning, Snackbar.LENGTH_LONG)
                        .setDuration(9000)
                        .show()
                }
            }
        }
    }

    private fun renderLocalFailure(result: LocalAuthResult) {
        when (result) {
            is LocalAuthResult.InvalidFields -> {
                binding.tilLocalName.error = result.errors.name?.let(::fieldMessage)
                binding.tilCode.error = result.errors.code?.let(::fieldMessage)
                binding.tilPin.error = result.errors.pin?.let(::fieldMessage)
            }
            LocalAuthResult.CodeAlreadyRegistered -> {
                registeringLocal = false
                renderMode()
                binding.tilCode.error = getString(R.string.auth_err_code_exists)
            }
            LocalAuthResult.DeviceHasOtherProfile ->
                Snackbar.make(binding.root, R.string.auth_err_device_profile, Snackbar.LENGTH_LONG).show()
            is LocalAuthResult.InvalidCredentials -> {
                val message = result.attemptsLeft
                    ?.let { getString(R.string.auth_err_attempts_left, it) }
                    ?: getString(R.string.auth_err_invalid_credentials)
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            }
            is LocalAuthResult.Locked -> startCountdown(result.remainingMillis)
            is LocalAuthResult.Success -> Unit
        }
        viewModel.consumeMessage()
    }

    private fun fieldMessage(error: FieldError): String = getString(
        when (error) {
            FieldError.NAME_REQUIRED -> R.string.auth_err_name_required
            FieldError.CODE_REQUIRED -> R.string.auth_err_code_required
            FieldError.CODE_TOO_SHORT -> R.string.auth_err_code_too_short
            FieldError.CODE_INVALID_CHARS -> R.string.auth_err_code_invalid
            FieldError.PIN_INVALID -> R.string.auth_err_pin_invalid
        }
    )

    private fun clearLocalErrors() {
        binding.tilLocalName.error = null
        binding.tilCode.error = null
        binding.tilPin.error = null
    }

    /** Cuenta atrás de 60 s tras 5 intentos fallidos (HU-04 CA-2). Usa reloj monotónico. */
    private fun startCountdown(remainingMillis: Long) {
        countdownJob?.cancel()
        val end = SystemClock.elapsedRealtime() + remainingMillis
        val job = viewLifecycleOwner.lifecycleScope.launch {
            binding.tvLocalStatus.isVisible = true
            while (true) {
                val left = ((end - SystemClock.elapsedRealtime() + 999) / 1000).toInt()
                if (left <= 0) break
                binding.tvLocalStatus.text = getString(R.string.auth_locked_countdown, left)
                delay(250)
            }
            binding.tvLocalStatus.isVisible = false
        }
        countdownJob = job
        job.invokeOnCompletion { _binding?.let { updateLocalSubmitEnabled() } }
        updateLocalSubmitEnabled()
    }

    private fun updateLocalSubmitEnabled() {
        binding.btnLocalSubmit.isEnabled = !loading && countdownJob?.isActive != true
    }
}