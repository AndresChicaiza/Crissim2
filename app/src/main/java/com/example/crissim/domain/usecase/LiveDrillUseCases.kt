package com.example.crissim.domain.usecase

import com.example.crissim.data.repository.LiveDrillRepository
import com.example.crissim.domain.model.DrillIncident
import com.example.crissim.domain.model.DrillStep
import com.example.crissim.domain.model.LiveDrill
import com.example.crissim.domain.model.UserRole
import java.util.UUID

/** Obtiene la guía de pasos según el rol del usuario (Docente vs Brigadista). */
class GetDrillGuideUseCase {
    fun execute(role: UserRole): List<DrillStep> {
        return when (role) {
            UserRole.TEACHER -> listOf(
                DrillStep(
                    stepNumber = 1,
                    title = "🚨 Activar Alarma e Iniciar Calma",
                    description = "Ordena a los estudiantes suspender actividades, apagar equipos y guardar silencio inmediatamente.",
                    roleAction = "DOCENTE: Mantener el liderazgo en el aula y transmitir serenidad."
                ),
                DrillStep(
                    stepNumber = 2,
                    title = "📋 Conteo Rápido de Estudiantes",
                    description = "Realiza la verificación visual de la lista de estudiantes presentes en el aula.",
                    roleAction = "DOCENTE: Llevar consigo la carpeta/planilla con la lista de asistencia."
                ),
                DrillStep(
                    stepNumber = 3,
                    title = "🚶 Guiar Evacuación por Ruta Segura",
                    description = "Encabeza la fila y dirige al grupo caminando sin correr hacia la salida de emergencia.",
                    roleAction = "DOCENTE: Asegurar que ningún estudiante retroceda ni lleve objetos pesados."
                ),
                DrillStep(
                    stepNumber = 4,
                    title = "📍 Conteo en Punto de Encuentro (P)",
                    description = "Reagrupa a los estudiantes en la zona P asignada y realiza el conteo final.",
                    roleAction = "DOCENTE: Reportar al brigadista el número de estudiantes evacuados y novedades."
                )
            )
            UserRole.BRIGADIST -> listOf(
                DrillStep(
                    stepNumber = 1,
                    title = "🚨 Alarma e Início de Coordenación",
                    description = "Verifica la sirena e inicia el control del bloque asignado.",
                    roleAction = "BRIGADISTA: Portar chaleco identificativo y equipo de comunicación."
                ),
                DrillStep(
                    stepNumber = 2,
                    title = "🚧 Despeje y Control de Pasillos",
                    description = "Dirige el flujo de personas y verifica que las rutas de evacuación estén libres.",
                    roleAction = "BRIGADISTA: Guiar grupos hacia las salidas de emergencia."
                ),
                DrillStep(
                    stepNumber = 3,
                    title = "🔍 Inspección de Aulas y Zonas Críticas",
                    description = "Revisa rápidamente aulas, laboratorios y baños para descartar personas atrapadas.",
                    roleAction = "BRIGADISTA: Cerrar puertas marcadas sin llave al verificar."
                ),
                DrillStep(
                    stepNumber = 4,
                    title = "📻 Reporte al Puesto de Mando (PMI)",
                    description = "Consolida tiempos de evacuación, conteos de docentes y novedades observadas.",
                    roleAction = "BRIGADISTA: Entregar el reporte de incidencias al coordinador del ILC."
                )
            )
            else -> listOf(
                DrillStep(
                    stepNumber = 1,
                    title = "🚨 Alarma de Evacuación",
                    description = "Suspende tareas y prepárate para salir en orden.",
                    roleAction = "PERSONAL: Conservar la calma y seguir la ruta indicada."
                ),
                DrillStep(
                    stepNumber = 2,
                    title = "🚶 Desplazamiento Seguro",
                    description = "Sigue la señalización verde hacia el punto de encuentro.",
                    roleAction = "PERSONAL: No usar ascensores ni correr."
                ),
                DrillStep(
                    stepNumber = 3,
                    title = "📍 Reporte en Punto de Encuentro",
                    description = "Permanecer en la zona segura hasta recibir autorización.",
                    roleAction = "PERSONAL: Confirmar asistencia con el líder de área."
                )
            )
        }
    }
}

/** Inicia una nueva sesión de Simulacro en Vivo. */
class StartLiveDrillUseCase {
    fun execute(userId: Long, userName: String, role: UserRole): LiveDrill {
        val drillId = "drill_" + UUID.randomUUID().toString().substring(0, 8)
        return LiveDrill(
            id = drillId,
            userId = userId,
            userName = userName,
            role = role,
            startedAt = System.currentTimeMillis(),
            targetTimeSeconds = 180 // Meta ILC: 3 min
        )
    }
}

/** Finaliza el simulacro y guarda el informe en el repositorio. */
class FinishLiveDrillUseCase(
    private val repository: LiveDrillRepository
) {
    suspend fun execute(activeDrill: LiveDrill, incidents: List<DrillIncident>): LiveDrill {
        val now = System.currentTimeMillis()
        val durationSeconds = ((now - activeDrill.startedAt) / 1000L).coerceAtLeast(1L)
        val completedDrill = activeDrill.copy(
            completedAt = now,
            totalDurationSeconds = durationSeconds,
            incidents = incidents,
            isCompleted = true
        )
        repository.saveDrill(completedDrill)
        return completedDrill
    }
}
