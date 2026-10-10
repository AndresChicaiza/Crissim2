package com.example.crissim.presentation.ui.profile

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
import com.example.crissim.databinding.FragmentProfileSetupBinding
import com.example.crissim.domain.model.UserRole
import kotlinx.coroutines.launch

class ProfileSetupFragment : Fragment() {

    private var _binding: FragmentProfileSetupBinding? = null
    private val binding get() = _binding!!

    private var selectedRole = UserRole.STUDENT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyStatusBarInset()
        setupRoleSelection()
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

    private fun setupRoleSelection() {
        binding.cardRoleStudent.setOnClickListener {
            selectedRole = UserRole.STUDENT
            updateRoleUi()
        }
        binding.cardRoleTeacher.setOnClickListener {
            selectedRole = UserRole.TEACHER
            updateRoleUi()
        }
        binding.cardRoleAdministrative.setOnClickListener {
            selectedRole = UserRole.ADMINISTRATIVE
            updateRoleUi()
        }
        binding.cardRoleBrigadist.setOnClickListener {
            selectedRole = UserRole.BRIGADIST
            updateRoleUi()
        }
    }

    private fun updateRoleUi() {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary)
        val defaultBorderColor = ContextCompat.getColor(requireContext(), R.color.primary_border)

        binding.cardRoleStudent.strokeColor = if (selectedRole == UserRole.STUDENT) primaryColor else defaultBorderColor
        binding.cardRoleStudent.strokeWidth = if (selectedRole == UserRole.STUDENT) 4 else 2
        binding.rbRoleStudent.isChecked = selectedRole == UserRole.STUDENT

        binding.cardRoleTeacher.strokeColor = if (selectedRole == UserRole.TEACHER) primaryColor else defaultBorderColor
        binding.cardRoleTeacher.strokeWidth = if (selectedRole == UserRole.TEACHER) 4 else 2
        binding.rbRoleTeacher.isChecked = selectedRole == UserRole.TEACHER

        binding.cardRoleAdministrative.strokeColor = if (selectedRole == UserRole.ADMINISTRATIVE) primaryColor else defaultBorderColor
        binding.cardRoleAdministrative.strokeWidth = if (selectedRole == UserRole.ADMINISTRATIVE) 4 else 2
        binding.rbRoleAdministrative.isChecked = selectedRole == UserRole.ADMINISTRATIVE

        binding.cardRoleBrigadist.strokeColor = if (selectedRole == UserRole.BRIGADIST) primaryColor else defaultBorderColor
        binding.cardRoleBrigadist.strokeWidth = if (selectedRole == UserRole.BRIGADIST) 4 else 2
        binding.rbRoleBrigadist.isChecked = selectedRole == UserRole.BRIGADIST
    }

    private fun setupButtons() {
        binding.btnCreateProfile.setOnClickListener {
            val name = binding.etName.text?.toString().orEmpty().trim()
            if (name.isEmpty()) {
                binding.tilName.error = getString(R.string.home_name_error)
                return@setOnClickListener
            }
            binding.tilName.error = null

            viewLifecycleOwner.lifecycleScope.launch {
                val userRepo = CrisisSimApp.instance.container.userRepository
                userRepo.createUser(name = name, role = selectedRole)
                findNavController().navigate(R.id.homeFragment)
            }
        }
    }
}
