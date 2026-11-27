package com.example.crissim.data.repository.impl

import com.example.crissim.data.local.dao.LiveDrillDao
import com.example.crissim.data.local.entity.LiveDrillEntity
import com.example.crissim.data.repository.LiveDrillRepository
import com.example.crissim.domain.model.DrillIncident
import com.example.crissim.domain.model.LiveDrill
import com.example.crissim.domain.model.UserRole
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación del repositorio de Simulacro en Vivo con Room y Gson.
 */
class LiveDrillRepositoryImpl(
    private val liveDrillDao: LiveDrillDao
) : LiveDrillRepository {

    private val gson = Gson()

    override suspend fun saveDrill(drill: LiveDrill) = withContext(Dispatchers.IO) {
        val entity = LiveDrillEntity(
            id = drill.id,
            userId = drill.userId,
            userName = drill.userName,
            role = drill.role.name,
            startedAt = drill.startedAt,
            completedAt = drill.completedAt,
            totalDurationSeconds = drill.totalDurationSeconds,
            targetTimeSeconds = drill.targetTimeSeconds,
            incidentsJson = gson.toJson(drill.incidents),
            isCompleted = drill.isCompleted
        )
        liveDrillDao.insertDrill(entity)
    }

    override suspend fun getDrillById(id: String): LiveDrill? = withContext(Dispatchers.IO) {
        liveDrillDao.getDrillById(id)?.toDomain()
    }

    override suspend fun getDrillsForUser(userId: Long): List<LiveDrill> = withContext(Dispatchers.IO) {
        liveDrillDao.getDrillsForUser(userId).map { it.toDomain() }
    }

    private fun LiveDrillEntity.toDomain(): LiveDrill {
        val roleEnum = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.TEACHER)
        val incidentsType = object : TypeToken<List<DrillIncident>>() {}.type
        val incidentsList: List<DrillIncident> = if (incidentsJson.isNotEmpty()) {
            runCatching { gson.fromJson<List<DrillIncident>>(incidentsJson, incidentsType) }.getOrNull() ?: emptyList()
        } else emptyList()

        return LiveDrill(
            id = id,
            userId = userId,
            userName = userName,
            role = roleEnum,
            startedAt = startedAt,
            completedAt = completedAt,
            totalDurationSeconds = totalDurationSeconds,
            targetTimeSeconds = targetTimeSeconds,
            incidents = incidentsList,
            isCompleted = isCompleted
        )
    }
}
