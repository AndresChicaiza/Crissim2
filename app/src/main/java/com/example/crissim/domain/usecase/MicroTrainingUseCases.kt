package com.example.crissim.domain.usecase

import com.example.crissim.data.repository.MicroTrainingRepository
import com.example.crissim.domain.model.Badge
import com.example.crissim.domain.model.BadgeCategory
import com.example.crissim.domain.model.DifficultyLevel
import com.example.crissim.domain.model.MicroOption
import com.example.crissim.domain.model.MicroTraining
import com.example.crissim.domain.model.MicroTrainingStatus
import com.example.crissim.domain.model.MicroTrainingWithProgress
import com.example.crissim.domain.model.Sm2Data
import com.example.crissim.domain.model.Streak
import com.example.crissim.domain.model.UserBadge
import com.example.crissim.domain.model.UserMicroTraining
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.max

/** Resultado de enviar respuesta a un micro-entrenamiento. */
data class MicroTrainingAnswerResult(
    val isCorrect: Boolean,
    val selectedOption: MicroOption,
    val explanation: String,
    val updatedSm2Data: Sm2Data,
    val streak: Streak,
    val newlyEarnedBadges: List<Badge>
)

/** UseCase para obtener los micro-entrenamientos disponibles o en revisión. */
class GetNextMicroTrainingUseCase(
    private val repository: MicroTrainingRepository
) {
    suspend fun execute(userId: Long): MicroTrainingWithProgress? {
        val now = System.currentTimeMillis()
        val due = repository.getDueForReview(userId, now)
        if (due.isNotEmpty()) {
            return due.first()
        }
        val all = repository.getMicroTrainingsWithProgress(userId)
        return all.firstOrNull()
    }
}

/** UseCase para enviar la respuesta y aplicar la repetición espaciada (SM-2) + Rachas e Insignias. */
class SubmitMicroAnswerUseCase(
    private val repository: MicroTrainingRepository
) {
    suspend fun execute(
        userId: Long,
        microTrainingId: Long,
        selectedOptionIndex: Int,
        userRatingQuality: Int? = null
    ): MicroTrainingAnswerResult? {
        val microTraining = repository.getMicroTrainingById(microTrainingId) ?: return null
        val options = microTraining.options
        if (selectedOptionIndex !in options.indices) return null

        val selectedOption = options[selectedOptionIndex]
        val isCorrect = selectedOption.isCorrect || selectedOptionIndex == microTraining.correctOptionIndex

        val existingUserProgress = repository.getUserMicroTraining(userId, microTrainingId)
        val currentSm2 = existingUserProgress?.sm2Data ?: Sm2Data()

        // Calidad SM-2 (0 a 5): si es correcto se asigna la calificación elegida (default 5), si es incorrecto 1.
        val quality = if (isCorrect) (userRatingQuality ?: 5).coerceIn(3, 5) else 1

        val newSm2 = calculateSm2(currentSm2, quality)
        val newStatus = if (newSm2.interval >= 30) {
            MicroTrainingStatus.MASTERED
        } else {
            MicroTrainingStatus.COMPLETED
        }

        val now = System.currentTimeMillis()
        val updatedUserProgress = UserMicroTraining(
            id = existingUserProgress?.id ?: 0,
            userId = userId,
            microTrainingId = microTrainingId,
            status = newStatus,
            attempts = (existingUserProgress?.attempts ?: 0) + 1,
            bestScore = if (isCorrect) max(existingUserProgress?.bestScore ?: 0, 100) else (existingUserProgress?.bestScore ?: 0),
            lastAttemptAt = now,
            completedAt = if (isCorrect && (existingUserProgress?.completedAt ?: 0) == 0L) now else (existingUserProgress?.completedAt ?: now),
            masteredAt = if (newStatus == MicroTrainingStatus.MASTERED && (existingUserProgress?.masteredAt ?: 0) == 0L) now else (existingUserProgress?.masteredAt ?: 0),
            sm2Data = newSm2
        )
        repository.saveUserMicroTraining(updatedUserProgress)

        // Actualizar racha e insignias ÚNICAMENTE si la respuesta es correcta
        val updatedStreak = if (isCorrect) {
            updateStreak(userId, now)
        } else {
            repository.getStreak(userId) ?: Streak(userId = userId)
        }

        val newlyEarnedBadges = if (isCorrect) {
            evaluateBadges(userId, updatedStreak)
        } else {
            emptyList()
        }

        return MicroTrainingAnswerResult(
            isCorrect = isCorrect,
            selectedOption = selectedOption,
            explanation = microTraining.explanation,
            updatedSm2Data = newSm2,
            streak = updatedStreak,
            newlyEarnedBadges = newlyEarnedBadges
        )
    }

    private fun calculateSm2(current: Sm2Data, quality: Int): Sm2Data {
        val q = quality.coerceIn(0, 5)
        var repetitions = current.repetitions
        var interval = current.interval
        var easeFactor = current.easeFactor

        if (q >= 3) {
            when (repetitions) {
                0 -> interval = 1
                1 -> interval = 6
                else -> interval = ceil(interval * easeFactor).toInt()
            }
            repetitions += 1
        } else {
            repetitions = 0
            interval = 1
        }

        // Ajuste del factor de facilidad (Ease Factor)
        easeFactor += (0.1f - (5 - q) * (0.08f + (5 - q) * 0.02f))
        if (easeFactor < 1.3f) {
            easeFactor = 1.3f
        }

        val nextReviewDate = System.currentTimeMillis() + interval * 24 * 60 * 60 * 1000L

        return Sm2Data(
            easeFactor = easeFactor,
            interval = interval,
            repetitions = repetitions,
            nextReviewDate = nextReviewDate,
            lastQuality = q
        )
    }

    private suspend fun updateStreak(userId: Long, now: Long): Streak {
        val currentStreakObj = repository.getStreak(userId) ?: Streak(userId = userId)
        val calendar = Calendar.getInstance()

        calendar.timeInMillis = now
        val currentDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = currentStreakObj.lastPracticeDate
        val lastDayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val lastYear = calendar.get(Calendar.YEAR)

        val isSameDay = (currentStreakObj.lastPracticeDate > 0) && (currentYear == lastYear) && (currentDayOfYear == lastDayOfYear)
        val isYesterday = if (currentStreakObj.lastPracticeDate > 0) {
            calendar.timeInMillis = currentStreakObj.lastPracticeDate
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            (calendar.get(Calendar.YEAR) == currentYear) && (calendar.get(Calendar.DAY_OF_YEAR) == currentDayOfYear)
        } else false

        var currentStreak = currentStreakObj.currentStreak
        var totalDays = currentStreakObj.totalPracticeDays

        if (!isSameDay) {
            totalDays += 1
            currentStreak = if (isYesterday || currentStreakObj.lastPracticeDate == 0L) {
                currentStreak + 1
            } else {
                1
            }
        }

        val longestStreak = max(currentStreakObj.longestStreak, currentStreak)
        val updatedStreak = Streak(
            userId = userId,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lastPracticeDate = now,
            totalPracticeDays = totalDays,
            updatedAt = now
        )

        repository.saveStreak(updatedStreak)
        return updatedStreak
    }

    private suspend fun evaluateBadges(userId: Long, streak: Streak): List<Badge> {
        val earnedBadges = mutableListOf<Badge>()
        val existingBadges = repository.getUserBadges(userId).map { it.badgeId }.toSet()

        val checkBadge = suspend { badgeId: String, condition: Boolean ->
            if (condition && badgeId !in existingBadges) {
                repository.insertUserBadge(UserBadge(userId = userId, badgeId = badgeId))
                repository.getBadgeById(badgeId)?.let { earnedBadges.add(it) }
            }
        }

        checkBadge("badge_first_step", streak.totalPracticeDays >= 1)
        checkBadge("badge_streak_3", streak.currentStreak >= 3)
        checkBadge("badge_streak_7", streak.currentStreak >= 7)
        checkBadge("badge_streak_30", streak.currentStreak >= 30)

        val userProgressList = repository.getUserMicroTrainings(userId)
        val masteredCount = userProgressList.count { it.status == MicroTrainingStatus.MASTERED }
        checkBadge("badge_master_protocol", masteredCount >= 3)

        return earnedBadges
    }
}

/** Seed de datos iniciales de micro-entrenamientos e insignias. */
class SeedMicroTrainingsUseCase(
    private val repository: MicroTrainingRepository
) {
    suspend fun execute() {
        if (repository.getAllBadges().isEmpty()) {
            val initialBadges = listOf(
                Badge(
                    id = "badge_first_step",
                    name = "Primer Paso",
                    description = "Completaste tu primer micro-entrenamiento diario.",
                    iconRes = "ic_badge_star",
                    category = BadgeCategory.CONSISTENCY,
                    requirement = "Completa 1 entrenamiento.",
                    order = 1
                ),
                Badge(
                    id = "badge_streak_3",
                    name = "Racha de 3 Días",
                    description = "Practicaste durante 3 días consecutivos.",
                    iconRes = "ic_badge_fire",
                    category = BadgeCategory.STREAK,
                    requirement = "Racha de 3 días.",
                    order = 2
                ),
                Badge(
                    id = "badge_streak_7",
                    name = "Constancia de Acero",
                    description = "Practicaste durante 7 días consecutivos.",
                    iconRes = "ic_badge_shield",
                    category = BadgeCategory.STREAK,
                    requirement = "Racha de 7 días.",
                    order = 3
                ),
                Badge(
                    id = "badge_streak_30",
                    name = "Maestro Inquebrantable",
                    description = "Mantuviste una racha ininterrumpida de 30 días.",
                    iconRes = "ic_badge_crown",
                    category = BadgeCategory.STREAK,
                    requirement = "Racha de 30 días.",
                    order = 4
                ),
                Badge(
                    id = "badge_master_protocol",
                    name = "Dominio de Protocolos",
                    description = "Dominaste al menos 3 micro-entrenamientos con repetición espaciada.",
                    iconRes = "ic_badge_master",
                    category = BadgeCategory.PROTOCOL_MASTER,
                    requirement = "Domina 3 micro-entrenamientos.",
                    order = 5
                )
            )
            initialBadges.forEach { repository.insertBadge(it) }
        }

        if (repository.getAllActiveMicroTrainings().isEmpty()) {
            val seedTrainings = listOf(
                MicroTraining(
                    protocolId = "SIS-01",
                    title = "Triángulo de Cobertura en Sismos",
                    question = "¿Cuál es la acción prioritaria inmediata durante un movimiento sísmico fuerte estando dentro de un aula?",
                    options = listOf(
                        MicroOption(text = "Correr inmediatamente hacia las escaleras para salir", isCorrect = false, feedback = "Incorrecto. Correr durante el sismo aumenta el riesgo de caídas y lesiones por objetos colapsados."),
                        MicroOption(text = "Agacharse, cubrirse debajo de una mesa resistente y sujetarse", isCorrect = true, feedback = "¡Correcto! Agacharse, cubrirse y sujetarse es el protocolo estándar de protección en sismos."),
                        MicroOption(text = "Pararse al lado de un ventanal para observar el exterior", isCorrect = false, feedback = "Incorrecto. Los cristales de las ventanas pueden romperse y causar cortes graves."),
                        MicroOption(text = "Usar el ascensor de emergencia", isCorrect = false, feedback = "Incorrecto. Nunca se deben usar ascensores durante un sismo por riesgo de atrapamiento o corte eléctrico.")
                    ),
                    correctOptionIndex = 1,
                    explanation = "El protocolo de agacharse, cubrirse y sujetarse protege la cabeza y el torso de objetos que caen durante la sacudida sísmica.",
                    protocolSource = "Manual de Gestión de Riesgos de Desastres - Nivel Básico",
                    difficulty = DifficultyLevel.BASIC,
                    tags = listOf("sismo", "aula", "cobertura"),
                    order = 1
                ),
                MicroTraining(
                    protocolId = "INC-01",
                    title = "Uso Correcto del Extintor (Técnica PASS)",
                    question = "Al operar un extintor de incendios con la técnica PASS, ¿hacia dónde se debe apuntar la manguera o boquilla?",
                    options = listOf(
                        MicroOption(text = "A la parte superior de las llamas", isCorrect = false, feedback = "Incorrecto. Apuntar a la cima del fuego desperdicia el agente extintor."),
                        MicroOption(text = "A la base del fuego", isCorrect = true, feedback = "¡Correcto! Apuntar a la base ataca el material combustible que alimenta las llamas."),
                        MicroOption(text = "Al humo producido por el fuego", isCorrect = false, feedback = "Incorrecto. El humo es un gas residual y no apaga el origen del incendio."),
                        MicroOption(text = "En círculos sobre el aire", isCorrect = false, feedback = "Incorrecto. El agente debe dispersarse abanicando de lado a lado en la base del fuego.")
                    ),
                    correctOptionIndex = 1,
                    explanation = "La mnemotecnia PASS indica: Pull (Jalar el seguro), Aim (Apuntar a la base), Squeeze (Presionar la palanca), Sweep (Abanicar de lado a lado).",
                    protocolSource = "Norma de Seguridad contra Incendios NFPA 10",
                    difficulty = DifficultyLevel.BASIC,
                    tags = listOf("incendio", "extintor", "pass"),
                    order = 2
                ),
                MicroTraining(
                    protocolId = "HUM-01",
                    title = "Desplazamiento en Presencia de Humo Denso",
                    question = "Si al intentar evacuar encuentras un pasillo lleno de humo denso, ¿cómo debes desplazarte?",
                    options = listOf(
                        MicroOption(text = "Caminar erguido cubriéndote los ojos", isCorrect = false, feedback = "Incorrecto. Los gases tóxicos y el calor se acumulan cerca del techo."),
                        MicroOption(text = "Agacharse o gatear cerca del suelo donde el aire es más limpio", isCorrect = true, feedback = "¡Correcto! El aire más fresco y respirable se encuentra en los primeros 30-50 cm sobre el suelo."),
                        MicroOption(text = "Correr a máxima velocidad sin mirar", isCorrect = false, feedback = "Incorrecto. Correr a ciegas puede causar colisiones o desorientación fatal."),
                        MicroOption(text = "Quedarse de pie esperando a que se disipe el humo", isCorrect = false, feedback = "Incorrecto. El humo de incendio contiene monóxido de carbono e inactiva la respiración en pocos segundos.")
                    ),
                    correctOptionIndex = 1,
                    explanation = "El calor y los gases tóxicos del humo ascienden. Arrastrarse o gatear evita la inhalación de gases mortales durante la evacuación.",
                    protocolSource = "Protocolo Institucional de Evacuación de Humo",
                    difficulty = DifficultyLevel.INTERMEDIATE,
                    tags = listOf("humo", "evacuacion", "seguridad"),
                    order = 3
                )
            )
            repository.insertMicroTrainings(seedTrainings)
        }
    }
}
