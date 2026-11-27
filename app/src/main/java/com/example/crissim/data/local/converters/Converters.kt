package com.example.crissim.data.local.converters

import androidx.room.TypeConverter
import com.example.crissim.domain.model.BadgeCategory
import com.example.crissim.domain.model.DifficultyLevel
import com.example.crissim.domain.model.MicroOption
import com.example.crissim.domain.model.MicroTrainingStatus
import com.example.crissim.domain.model.Sm2Data
import com.google.gson.Gson

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromMicroOptionList(value: String?): List<MicroOption> {
        if (value.isNullOrEmpty()) return emptyList()
        val array = gson.fromJson(value, Array<MicroOption>::class.java)
        return array?.toList() ?: emptyList()
    }

    @TypeConverter
    fun toMicroOptionList(list: List<MicroOption>?): String {
        return gson.toJson(list ?: emptyList<MicroOption>())
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        val array = gson.fromJson(value, Array<String>::class.java)
        return array?.toList() ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromSm2Data(value: String?): Sm2Data {
        if (value.isNullOrEmpty()) return Sm2Data()
        return gson.fromJson(value, Sm2Data::class.java) ?: Sm2Data()
    }

    @TypeConverter
    fun toSm2Data(sm2Data: Sm2Data?): String {
        return gson.toJson(sm2Data ?: Sm2Data())
    }

    @TypeConverter
    fun fromDifficulty(value: String?): DifficultyLevel {
        return value?.let {
            runCatching { DifficultyLevel.valueOf(it) }.getOrNull()
        } ?: DifficultyLevel.BASIC
    }

    @TypeConverter
    fun toDifficulty(difficulty: DifficultyLevel?): String {
        return (difficulty ?: DifficultyLevel.BASIC).name
    }

    @TypeConverter
    fun fromStatus(value: String?): MicroTrainingStatus {
        return value?.let {
            runCatching { MicroTrainingStatus.valueOf(it) }.getOrNull()
        } ?: MicroTrainingStatus.PENDING
    }

    @TypeConverter
    fun toStatus(status: MicroTrainingStatus?): String {
        return (status ?: MicroTrainingStatus.PENDING).name
    }

    @TypeConverter
    fun fromBadgeCategory(value: String?): BadgeCategory {
        return value?.let {
            runCatching { BadgeCategory.valueOf(it) }.getOrNull()
        } ?: BadgeCategory.STREAK
    }

    @TypeConverter
    fun toBadgeCategory(category: BadgeCategory?): String {
        return (category ?: BadgeCategory.STREAK).name
    }
}
