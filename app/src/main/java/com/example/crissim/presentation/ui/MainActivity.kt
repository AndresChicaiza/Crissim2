package com.example.crissim.presentation.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.crissim.R
import com.example.crissim.databinding.ActivityMainBinding

/**
 * Activity principal de CrisisSim.
 * Single Activity Architecture con Navigation Component.
 *
 * onCreate solo inicializa: toda la lógica está en funciones separadas.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeBinding()
        initializeNavigation()
        configureBottomNavigation()
    }

    // ─── Funciones de inicialización ───

    /**
     * Infla el layout con ViewBinding.
     */
    private fun initializeBinding() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    /**
     * Configura el NavController desde el NavHostFragment.
     */
    private fun initializeNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    /**
     * Conecta el BottomNavigationView con el NavController.
     */
    private fun configureBottomNavigation() {
        binding.bottomNavigation.setupWithNavController(navController)

        // Ocultar el bottom nav en pantallas de escenario
        navController.addOnDestinationChangedListener { _, destination, _ ->
            handleBottomNavVisibility(destination.id)
        }
    }

    /**
     * Controla en qué pantallas se muestra la barra de navegación inferior.
     */
    private fun handleBottomNavVisibility(destinationId: Int) {
        val showBottomNav = when (destinationId) {
            R.id.homeFragment,
            R.id.levelsFragment,
            R.id.mapFragment,
            R.id.certificatesFragment -> true
            else -> false
        }
        binding.bottomNavigation.visibility = if (showBottomNav) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }

    /**
     * Delega la navegación hacia atrás al NavController.
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
