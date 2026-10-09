package com.example.crissim

import android.app.Application
import com.example.crissim.data.local.CrisisSimDatabase

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

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CrisisSimApp
            private set
    }
}