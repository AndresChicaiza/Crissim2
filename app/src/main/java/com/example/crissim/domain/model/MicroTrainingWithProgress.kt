package com.example.crissim.domain.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * MicroTraining con progreso del usuario combinado para UI.
 */
data class MicroTrainingWithProgress(
    @Embedded val microTraining: MicroTraining,
    @Relation(
        parentColumn = "id",
        entityColumn = "microTrainingId",
        entity = UserMicroTraining::class
    )
    val userProgress: UserMicroTraining?
)