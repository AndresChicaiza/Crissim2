package com.example.crissim.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.crissim.data.local.dao.*
import com.example.crissim.data.local.entity.*

/**
 * Base de datos Room de CrisisSim.
 * A7: Solo almacena nombre y resultados. Datos mínimos.
 */
@Database(
    entities = [
        UserEntity::class,
        AttemptEntity::class,
        LevelProgressEntity::class,
        CertificateEntity::class,
        SavedStateEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class CrisisSimDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun attemptDao(): AttemptDao
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun certificateDao(): CertificateDao
    abstract fun savedStateDao(): SavedStateDao

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
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
