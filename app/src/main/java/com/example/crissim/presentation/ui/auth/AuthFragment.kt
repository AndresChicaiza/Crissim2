package com.example.crissim.presentation.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.crissim.presentation.viewmodel.AuthUiState
import com.example.crissim.presentation.viewmodel.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { AuthViewModel.Factory }
    private var registering = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupActions()
        renderMode()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::renderState)
            }
        }
        viewModel.restoreSession()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupActions() {
        binding.btnAuthToggle.setOnClickListener {
            registering = !registering
            renderMode()
        }
        binding.btnAuthSubmit.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty()
            val password = binding.etPassword.text?.toString().orEmpty()
            if (registering) {
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
        binding.tilName.isVisible = registering
        binding.btnForgotPassword.isVisible = !registering
        binding.tvAuthTitle.setText(if (registering) R.string.auth_title_register else R.string.auth_title_login)
        binding.btnAuthSubmit.setText(if (registering) R.string.auth_register_button else R.string.auth_login_button)
        binding.btnAuthToggle.setText(if (registering) R.string.auth_switch_login else R.string.auth_switch_register)
    }

    private fun renderState(state: AuthUiState) {
        binding.progressAuth.isVisible = state is AuthUiState.Loading
        binding.btnAuthSubmit.isEnabled = state !is AuthUiState.Loading
        binding.btnAuthToggle.isEnabled = state !is AuthUiState.Loading
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
}
