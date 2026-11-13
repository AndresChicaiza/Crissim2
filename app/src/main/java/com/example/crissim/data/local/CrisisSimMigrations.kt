package com.example.crissim.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Explicit, data-preserving Room upgrades. Historical level_progress rows are copied intact.
 * Attempts/certificates receive stable UUIDs; scenario progress is backfilled after startup,
 * when the local JSON catalog is available.
 */
object CrisisSimMigrations {
    val MIGRATION_1_3 = object : Migration(1, 3) {
        override fun migrate(db: SupportSQLiteDatabase) = rebuildToVersion3(db, oldVersion = 1)
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) = rebuildToVersion3(db, oldVersion = 2)
    }

    private fun rebuildToVersion3(db: SupportSQLiteDatabase, oldVersion: Int) {
        listOf("users", "attempts", "level_progress", "certificates", "saved_states")
            .forEach { table -> db.execSQL("CREATE TABLE migration_backup_$table AS SELECT * FROM $table") }

        // Drop child tables first so their foreign keys never cascade-delete historical rows.
        listOf("saved_states", "certificates", "level_progress", "attempts", "users")
            .forEach { table -> db.execSQL("DROP TABLE $table") }

        createVersion3Tables(db)
        if (oldVersion == 1) {
            db.execSQL(
                """INSERT INTO users (id, name, role, language, createdAt, accountType, firebaseUid)
                   SELECT id, name, 'STUDENT', 'ES', createdAt, 'GUEST', NULL
                   FROM migration_backup_users""".trimIndent()
            )
        } else {
            db.execSQL(
                """INSERT INTO users (id, name, role, language, createdAt, accountType, firebaseUid)
                   SELECT id, name, role, language, createdAt, 'GUEST', NULL
                   FROM migration_backup_users""".trimIndent()
            )
        }

        db.execSQL(
            """INSERT INTO attempts
               (id, attemptUuid, scenarioId, userId, score, maxScore, isPassed, startedAt,
                completedAt, endNodeId, endType, decisionsJson, scenarioVersion)
               SELECT id,
                lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-' ||
                      hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(6))),
                scenarioId, userId, score, maxScore, isPassed, startedAt, completedAt,
                endNodeId, endType, decisionsJson, NULL
               FROM migration_backup_attempts""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO level_progress
               (id, userId, level, isCompleted, bestScore, maxScore, attempts, lastAttemptAt)
               SELECT id, userId, level, isCompleted, bestScore, maxScore, attempts, lastAttemptAt
               FROM migration_backup_level_progress""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO certificates
               (id, certificateUuid, userId, userName, level, scenarioId, score, maxScore,
                issuedAt, signature, pdfPath)
               SELECT id,
                lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-' ||
                      hex(randomblob(2)) || '-' || hex(randomblob(2)) || '-' || hex(randomblob(6))),
                userId, userName, level, scenarioId, score, maxScore, issuedAt, signature, pdfPath
               FROM migration_backup_certificates""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO certificate_files (certificateUuid, localUri, remoteObjectPath, sha256, mimeType)
               SELECT certificateUuid, pdfPath, NULL, NULL, 'application/pdf'
               FROM certificates WHERE pdfPath IS NOT NULL""".trimIndent()
        )
        db.execSQL(
            """INSERT INTO saved_states
               (id, userId, scenarioId, currentNodeId, score, decisionsJson, startTime, savedAt,
                deviceId, revision)
               SELECT id, userId, scenarioId, currentNodeId, score, decisionsJson, startTime,
                savedAt, 'legacy-device', 0
               FROM migration_backup_saved_states""".trimIndent()
        )

        listOf("users", "attempts", "level_progress", "certificates", "saved_states")
            .forEach { table -> db.execSQL("DROP TABLE migration_backup_$table") }
    }

    private fun createVersion3Tables(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE users (
               id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
               name TEXT NOT NULL, role TEXT NOT NULL, language TEXT NOT NULL, createdAt INTEGER NOT NULL,
               accountType TEXT NOT NULL DEFAULT 'GUEST', firebaseUid TEXT)""".trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_users_firebaseUid ON users(firebaseUid)")

        db.execSQL(
            """CREATE TABLE attempts (
               id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
               attemptUuid TEXT NOT NULL DEFAULT '',
               scenarioId TEXT NOT NULL, userId INTEGER NOT NULL, score INTEGER NOT NULL,
               maxScore INTEGER NOT NULL, isPassed INTEGER NOT NULL, startedAt INTEGER NOT NULL,
               completedAt INTEGER NOT NULL, endNodeId TEXT NOT NULL, endType TEXT NOT NULL,
               decisionsJson TEXT NOT NULL, scenarioVersion TEXT,
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_attempts_userId ON attempts(userId)")
        db.execSQL("CREATE INDEX index_attempts_scenarioId ON attempts(scenarioId)")
        db.execSQL("CREATE UNIQUE INDEX index_attempts_attemptUuid ON attempts(attemptUuid)")

        db.execSQL(
            """CREATE TABLE level_progress (
               id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL,
               level TEXT NOT NULL, isCompleted INTEGER NOT NULL, bestScore INTEGER NOT NULL,
               maxScore INTEGER NOT NULL, attempts INTEGER NOT NULL, lastAttemptAt INTEGER,
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_level_progress_userId ON level_progress(userId)")
        db.execSQL("CREATE UNIQUE INDEX index_level_progress_userId_level ON level_progress(userId, level)")

        db.execSQL(
            """CREATE TABLE certificates (
               id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
               certificateUuid TEXT NOT NULL DEFAULT '',
               userId INTEGER NOT NULL, userName TEXT NOT NULL, level TEXT NOT NULL,
               scenarioId TEXT NOT NULL, score INTEGER NOT NULL, maxScore INTEGER NOT NULL,
               issuedAt INTEGER NOT NULL, signature TEXT NOT NULL, pdfPath TEXT,
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_certificates_userId ON certificates(userId)")
        db.execSQL("CREATE UNIQUE INDEX index_certificates_certificateUuid ON certificates(certificateUuid)")

        db.execSQL(
            """CREATE TABLE saved_states (
               id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL,
               scenarioId TEXT NOT NULL, currentNodeId TEXT NOT NULL, score INTEGER NOT NULL,
               decisionsJson TEXT NOT NULL, startTime INTEGER NOT NULL, savedAt INTEGER NOT NULL,
               deviceId TEXT NOT NULL DEFAULT 'legacy-device', revision INTEGER NOT NULL DEFAULT 0,
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX index_saved_states_userId_scenarioId_deviceId ON saved_states(userId, scenarioId, deviceId)")

        db.execSQL(
            """CREATE TABLE scenario_progress (
               userId INTEGER NOT NULL, scenarioId TEXT NOT NULL, level TEXT NOT NULL,
               isRequiredForLevel INTEGER NOT NULL, bestScore INTEGER NOT NULL, maxScore INTEGER NOT NULL,
               bestPercentage INTEGER NOT NULL, isCompleted INTEGER NOT NULL, bestAttemptUuid TEXT,
               bestAttemptAt INTEGER, lastAttemptAt INTEGER, scenarioVersion TEXT, updatedAt INTEGER NOT NULL,
               PRIMARY KEY(userId, scenarioId),
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_scenario_progress_scenarioId ON scenario_progress(scenarioId)")
        db.execSQL("CREATE INDEX index_scenario_progress_userId_level ON scenario_progress(userId, level)")

        db.execSQL(
            """CREATE TABLE scenario_progress_backfill (
               userId INTEGER NOT NULL PRIMARY KEY, completedAt INTEGER NOT NULL,
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL(
            """CREATE TABLE certificate_files (
               certificateUuid TEXT NOT NULL PRIMARY KEY, localUri TEXT, remoteObjectPath TEXT,
               sha256 TEXT, mimeType TEXT NOT NULL,
               FOREIGN KEY(certificateUuid) REFERENCES certificates(certificateUuid)
               ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_certificate_files_certificateUuid ON certificate_files(certificateUuid)")

        db.execSQL(
            """CREATE TABLE sync_outbox (
               operationUuid TEXT NOT NULL PRIMARY KEY, userId INTEGER NOT NULL,
               aggregateType TEXT NOT NULL, aggregateId TEXT NOT NULL, operation TEXT NOT NULL,
               payloadJson TEXT NOT NULL, createdAt INTEGER NOT NULL,
               status TEXT NOT NULL DEFAULT 'PENDING', attemptCount INTEGER NOT NULL,
               lastError TEXT,
               FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)""".trimIndent()
        )
        db.execSQL("CREATE INDEX index_sync_outbox_userId_status_createdAt ON sync_outbox(userId, status, createdAt)")
        db.execSQL("CREATE INDEX index_sync_outbox_aggregateId ON sync_outbox(aggregateId)")
    }
}
