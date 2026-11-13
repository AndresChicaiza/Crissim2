package com.example.crissim

import com.example.crissim.data.repository.CertificateRepository
import com.example.crissim.data.repository.ProgressRepository
import com.example.crissim.data.repository.ScenarioRepository
import com.example.crissim.data.repository.UserRepository
import com.example.crissim.data.repository.impl.CertificateRepositoryImpl
import com.example.crissim.data.repository.impl.ProgressRepositoryImpl
import com.example.crissim.data.repository.impl.ScenarioRepositoryImpl
import com.example.crissim.data.repository.impl.UserRepositoryImpl
import com.example.crissim.data.repository.impl.StudentCloudSync

/**
 * Contenedor simple de dependencias (sin librerías de inyección).
 * Crea los repositorios una sola vez y los comparte con las pantallas.
 */
class AppContainer(app: CrisisSimApp) {

    private val db = app.database

    val studentCloudSync: StudentCloudSync = StudentCloudSync(app, db)

    val userRepository: UserRepository = UserRepositoryImpl(db.userDao())

    val scenarioRepository: ScenarioRepository = ScenarioRepositoryImpl(app)

    val progressRepository: ProgressRepository = ProgressRepositoryImpl(
        db, db.attemptDao(), db.levelProgressDao(), db.scenarioProgressDao(),
        db.scenarioProgressBackfillDao(), scenarioRepository
    )

    val certificateRepository: CertificateRepository =
        CertificateRepositoryImpl(db.certificateDao())
}
