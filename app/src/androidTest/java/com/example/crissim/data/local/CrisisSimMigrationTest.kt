package com.example.crissim.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.crissim.data.local.entity.AttemptEntity
import com.example.crissim.data.local.entity.CertificateEntity
import com.example.crissim.data.local.entity.CertificateFileEntity
import com.example.crissim.data.local.entity.SavedStateEntity
import com.example.crissim.data.local.entity.ScenarioProgressEntity
import com.example.crissim.data.local.entity.SyncOutboxEntity
import com.example.crissim.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CrisisSimMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        CrisisSimDatabase::class.java
    )

    @Test fun migration1To3RetainsLegacyRowsAndAddsIdentity() = verifyMigration(1)
    @Test fun migration2To3RetainsLegacyRowsAndAddsIdentity() = verifyMigration(2)

    @Test fun version3EntitiesPersistAndKeepDeviceStatesSeparate() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, CrisisSimDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val userId = database.userDao().insertUser(UserEntity(name = "Estudiante"))
            val attempt = AttemptEntity(
                scenarioId = "fire-basic-01", userId = userId, score = 30, maxScore = 40,
                isPassed = true, startedAt = 1, completedAt = 2, endNodeId = "end",
                endType = "SUCCESS", decisionsJson = "[]"
            )
            database.attemptDao().insertAttempt(attempt)
            assertEquals(attempt.attemptUuid, database.attemptDao().getAttemptsByScenario(userId, attempt.scenarioId).single().attemptUuid)

            val certificate = CertificateEntity(
                userId = userId, userName = "Estudiante", level = "BASIC",
                scenarioId = attempt.scenarioId, score = 30, maxScore = 40
            )
            database.certificateDao().insertCertificate(certificate)
            database.certificateFileDao().upsert(CertificateFileEntity(certificate.certificateUuid, localUri = "file:///cert.pdf"))
            assertEquals("file:///cert.pdf", database.certificateFileDao().get(certificate.certificateUuid)?.localUri)

            database.scenarioProgressDao().upsert(
                ScenarioProgressEntity(userId, attempt.scenarioId, "BASIC", bestScore = 30, isCompleted = true)
            )
            assertEquals(true, database.scenarioProgressDao().get(userId, attempt.scenarioId)?.isCompleted)

            database.savedStateDao().saveState(SavedStateEntity(userId = userId, scenarioId = attempt.scenarioId, currentNodeId = "a", score = 5, decisionsJson = "[]", startTime = 1, deviceId = "device-a"))
            database.savedStateDao().saveState(SavedStateEntity(userId = userId, scenarioId = attempt.scenarioId, currentNodeId = "b", score = 7, decisionsJson = "[]", startTime = 1, deviceId = "device-b"))
            assertEquals(2, database.savedStateDao().getSavedStates(userId, attempt.scenarioId).size)

            database.syncOutboxDao().enqueue(SyncOutboxEntity(userId = userId, aggregateType = "attempt", aggregateId = attempt.attemptUuid, operation = "upsert", payloadJson = "{}"))
            assertEquals(1, database.syncOutboxDao().getPending(userId).size)
        } finally {
            database.close()
        }
    }

    private fun verifyMigration(oldVersion: Int) {
        val name = "migration-$oldVersion-test"
        val db = helper.createDatabase(name, oldVersion)
        if (oldVersion == 1) {
            db.execSQL("INSERT INTO users (id, name, createdAt) VALUES (1, 'Estudiante', 10)")
        } else {
            db.execSQL("INSERT INTO users (id, name, role, language, createdAt) VALUES (1, 'Estudiante', 'STUDENT', 'ES', 10)")
        }
        db.execSQL("INSERT INTO attempts (id, scenarioId, userId, score, maxScore, isPassed, startedAt, completedAt, endNodeId, endType, decisionsJson) VALUES (1, 'fire-basic-01', 1, 30, 40, 1, 10, 20, 'end', 'SUCCESS', '[]')")
        db.execSQL("INSERT INTO attempts (id, scenarioId, userId, score, maxScore, isPassed, startedAt, completedAt, endNodeId, endType, decisionsJson) VALUES (2, 'fire-basic-01', 1, 10, 40, 0, 21, 30, 'end', 'FAILURE', '[]')")
        db.execSQL("INSERT INTO level_progress (id, userId, level, isCompleted, bestScore, maxScore, attempts, lastAttemptAt) VALUES (1, 1, 'BASIC', 1, 30, 40, 1, 20)")
        db.execSQL("INSERT INTO certificates (id, userId, userName, level, scenarioId, score, maxScore, issuedAt, signature, pdfPath) VALUES (1, 1, 'Estudiante', 'BASIC', 'fire-basic-01', 30, 40, 20, 'sig', 'file:///legacy.pdf')")
        db.execSQL("INSERT INTO certificates (id, userId, userName, level, scenarioId, score, maxScore, issuedAt, signature, pdfPath) VALUES (2, 1, 'Estudiante', 'BASIC', 'fire-basic-01', 35, 40, 31, 'sig-2', NULL)")
        db.execSQL("INSERT INTO saved_states (id, userId, scenarioId, currentNodeId, score, decisionsJson, startTime, savedAt) VALUES (1, 1, 'fire-basic-01', 'node-2', 10, '[]', 10, 15)")
        db.close()

        val migrated = helper.runMigrationsAndValidate(
            name,
            3,
            true,
            CrisisSimMigrations.MIGRATION_1_3,
            CrisisSimMigrations.MIGRATION_2_3
        )
        assertEquals(1, count(migrated, "users"))
        assertEquals(2, count(migrated, "attempts"))
        assertEquals(1, count(migrated, "level_progress"))
        assertEquals(2, count(migrated, "certificates"))
        assertEquals(1, count(migrated, "saved_states"))
        assertEquals("GUEST", string(migrated, "SELECT accountType FROM users WHERE id=1"))
        assertNotEquals("", string(migrated, "SELECT attemptUuid FROM attempts WHERE id=1"))
        assertNotEquals("", string(migrated, "SELECT certificateUuid FROM certificates WHERE id=1"))
        assertEquals(2, queryInt(migrated, "SELECT COUNT(DISTINCT attemptUuid) FROM attempts"))
        assertEquals(2, queryInt(migrated, "SELECT COUNT(DISTINCT certificateUuid) FROM certificates"))
        assertEquals(0, queryRowCount(migrated, "PRAGMA foreign_key_check"))
        assertEquals("legacy-device", string(migrated, "SELECT deviceId FROM saved_states WHERE id=1"))
        assertEquals("file:///legacy.pdf", string(migrated, "SELECT localUri FROM certificate_files WHERE certificateUuid=(SELECT certificateUuid FROM certificates WHERE id=1)"))
        assertEquals(0, count(migrated, "scenario_progress"))
        // Summary row remains untouched; scenario projection is populated later from the JSON catalog.
        assertEquals(1, queryInt(migrated, "SELECT COUNT(*) FROM level_progress WHERE isCompleted=1"))
        migrated.close()
    }

    private fun count(db: SupportSQLiteDatabase, table: String): Int {
        return queryInt(db, "SELECT COUNT(*) FROM $table")
    }

    private fun queryInt(db: SupportSQLiteDatabase, sql: String): Int {
        db.query(sql).use { cursor -> cursor.moveToFirst(); return cursor.getInt(0) }
    }

    private fun queryRowCount(db: SupportSQLiteDatabase, sql: String): Int {
        db.query(sql).use { cursor -> return cursor.count }
    }

    private fun string(db: SupportSQLiteDatabase, sql: String): String {
        db.query(sql).use { cursor -> cursor.moveToFirst(); return cursor.getString(0) }
    }
}
