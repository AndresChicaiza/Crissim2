package com.example.crissim

import com.example.crissim.data.repository.CertificateRepository
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.data.repository.impl.CertificateRepositoryImpl
import com.example.crissim.data.repository.impl.ProgressRepositoryImpl
import com.example.crissim.data.repository.impl.ScenarioRepositoryImpl
import com.example.crissim.data.repository.impl.UserRepositoryImpl

/**
 * Contenedor simple de dependencias (sin librerías de inyección).
 * Crea los repositorios una sola vez y los comparte con las pantallas.
 */
class AppContainer(app: CrisisSimApp) {

    private val db = app.database

    val userRepository: UserRepository = UserRepositoryImpl(db.userDao())

    val progressRepository: ProgressRepository =
        ProgressRepositoryImpl(db.attemptDao(), db.levelProgressDao())

    val scenarioRepository: ScenarioRepository = ScenarioRepositoryImpl(app)

    val certificateRepository: CertificateRepository =
        CertificateRepositoryImpl(db.certificateDao())
}