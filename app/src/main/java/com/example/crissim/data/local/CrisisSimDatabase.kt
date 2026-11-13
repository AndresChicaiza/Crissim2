package com.example.crissim.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.crissim.data.local.dao.*
import com.example.crissim.data.local.entity.*

/**
 * Room persistence for local profiles, scenario progress, attempt history, certificates and sync preparation.
 */
@Database(
    entities = [
        UserEntity::class,
        AttemptEntity::class,
        LevelProgressEntity::class,
        CertificateEntity::class,
        SavedStateEntity::class,
        ScenarioProgressEntity::class,
        ScenarioProgressBackfillEntity::class,
        CertificateFileEntity::class,
        SyncOutboxEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class CrisisSimDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun attemptDao(): AttemptDao
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun certificateDao(): CertificateDao
    abstract fun savedStateDao(): SavedStateDao
    abstract fun scenarioProgressDao(): ScenarioProgressDao
    abstract fun scenarioProgressBackfillDao(): ScenarioProgressBackfillDao
    abstract fun certificateFileDao(): CertificateFileDao
    abstract fun syncOutboxDao(): SyncOutboxDao

    companion object {
        private const val DATABASE_NAME = "crisissim.db"

        @Volatile
        private var INSTANCE: CrisisSimDatabase? = null

        fun getInstance(context: Context): CrisisSimDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): CrisisSimDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                CrisisSimDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(CrisisSimMigrations.MIGRATION_1_3, CrisisSimMigrations.MIGRATION_2_3)
                .build()
        }
    }
}
