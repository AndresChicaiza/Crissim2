package com.example.crissim

import android.app.Application
import android.content.Context
import com.example.crissim.data.local.CrisisSimDatabase
import com.example.crissim.domain.usecase.SeedMicroTrainingsUseCase
import com.example.crissim.util.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase Application de CrisisSim.
 * Inicializa dependencias globales (base de datos, repositorios).
 * Se mantiene limpia: solo coordina la inicialización.
 */
class CrisisSimApp : Application() {

    /** Base de datos Room (lazy singleton). */
    val database: CrisisSimDatabase by lazy {
        CrisisSimDatabase.getInstance(this)
    }

    /** Repositorios compartidos por toda la app. */
    val container: AppContainer by lazy {
        AppContainer(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Aplicar locale guardado al inicio
        LocaleHelper.updateLocale(this, LocaleHelper.getLanguage(this))

        // Sembrar micro-entrenamientos e insignias iniciales offline
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SeedMicroTrainingsUseCase(container.microTrainingRepository).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        lateinit var instance: CrisisSimApp
            private set
    }
}
