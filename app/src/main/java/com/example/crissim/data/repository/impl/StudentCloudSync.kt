package com.example.crissim.data.repository.impl

import android.content.Context
import com.example.crissim.data.local.CrisisSimDatabase
import com.example.crissim.data.local.entity.AttemptEntity
import com.example.crissim.data.local.entity.CertificateEntity
import com.example.crissim.data.local.entity.LevelProgressEntity
import com.example.crissim.data.local.entity.SavedStateEntity
import com.example.crissim.data.local.entity.UserEntity
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocalAccountBindingException(message: String) : Exception(message)

/** Sincroniza Room como caché local y Firestore como almacenamiento compartido por UID. */
class StudentCloudSync(context: Context, private val db: CrisisSimDatabase) {
    private val firestore = FirebaseFirestore.getInstance()
    private val prefs = context.getSharedPreferences("student_cloud_sync", Context.MODE_PRIVATE)

    suspend fun synchronize(uid: String, defaultName: String, language: String = "ES") {
        val previousUid = prefs.getString(KEY_UID, null)
        if (previousUid != null && previousUid != uid) {
            throw LocalAccountBindingException(
                "Este dispositivo conserva progreso vinculado a otra cuenta para evitar mezclar estudiantes. Inicia la cuenta que ya usaste aqu\u00ed."
            )
        }

        val userRef = firestore.collection("users").document(uid)
        val profileRef = userRef.collection("profile").document("main")
        val remoteProfile = profileRef.get().awaitResult()
        var localUser = db.userDao().getActiveUser()

        if (remoteProfile.exists()) {
            val remote = remoteProfile.toProfile(uid)
            if (localUser == null) {
                val id = db.userDao().insertUser(remote.copy(id = 0))
                localUser = remote.copy(id = id)
            } else {
                db.userDao().updateUser(localUser!!.copy(
                    name = remote.name,
                    role = remote.role,
                    language = remote.language,
                    createdAt = remote.createdAt
                ))
                localUser = db.userDao().getActiveUser()
            }
        } else {
            if (localUser == null) {
                val id = db.userDao().insertUser(UserEntity(name = defaultName.ifBlank { "Estudiante" }, language = language))
                localUser = db.userDao().getUserById(id)
            }
            val profile = requireNotNull(localUser)
            profileRef.set(profile.toMap(), SetOptions.merge()).awaitResult()
        }

        val user = requireNotNull(localUser)
        val progressCollection = userRef.collection("progress")
        progressCollection.get().awaitResult().documents.forEach { doc ->
            val remote = doc.toProgress(user.id) ?: return@forEach
            val local = db.levelProgressDao().getProgress(user.id, remote.level)
            val merged = if (local == null) remote else remote.copy(
                isCompleted = local.isCompleted || remote.isCompleted,
                bestScore = maxOf(local.bestScore, remote.bestScore),
                maxScore = maxOf(local.maxScore, remote.maxScore),
                attempts = maxOf(local.attempts, remote.attempts),
                lastAttemptAt = maxOfNullable(local.lastAttemptAt, remote.lastAttemptAt)
            )
            db.levelProgressDao().insertOrUpdate(merged)
        }
        db.levelProgressDao().getAllProgress(user.id).forEach { progress ->
            progressCollection.document(progress.level).set(progress.toMap(), SetOptions.merge()).awaitResult()
        }

        val attemptsCollection = userRef.collection("attempts")
        attemptsCollection.get().awaitResult().documents.forEach { doc ->
            val remote = doc.toAttempt(user.id) ?: return@forEach
            if (db.attemptDao().getAttempt(user.id, remote.scenarioId, remote.completedAt) == null) {
                db.attemptDao().insertAttempt(remote.copy(id = 0))
            }
        }
        db.attemptDao().getAllAttempts(user.id).forEach { attempt ->
            attemptsCollection.document("${attempt.scenarioId}_${attempt.completedAt}")
                .set(attempt.toMap(), SetOptions.merge()).awaitResult()
        }

        val certificatesCollection = userRef.collection("certificates")
        certificatesCollection.get().awaitResult().documents.forEach { doc ->
            val remote = doc.toCertificate(user.id) ?: return@forEach
            if (db.certificateDao().getCertificate(user.id, remote.level, remote.scenarioId) == null) {
                db.certificateDao().insertCertificate(remote.copy(id = 0, pdfPath = null))
            }
        }
        db.certificateDao().getCertificatesByUser(user.id).forEach { certificate ->
            certificatesCollection.document("${certificate.level}_${certificate.scenarioId}_${certificate.issuedAt}")
                .set(certificate.toMap(), SetOptions.merge()).awaitResult()
        }

        val statesCollection = userRef.collection("savedStates")
        statesCollection.get().awaitResult().documents.forEach { doc ->
            val remote = doc.toSavedState(user.id) ?: return@forEach
            val local = db.savedStateDao().getSavedState(user.id, remote.scenarioId, remote.deviceId)
            if (local == null) db.savedStateDao().saveState(remote.copy(id = 0))
        }
        db.savedStateDao().getAllSavedStates(user.id).forEach { state ->
            statesCollection.document(state.scenarioId).set(state.toMap(), SetOptions.merge()).awaitResult()
        }

        prefs.edit().putString(KEY_UID, uid).apply()
    }

    private fun UserEntity.toMap() = mapOf(
        "name" to name, "role" to role, "language" to language, "createdAt" to createdAt
    )

    private fun DocumentSnapshot.toProfile(uid: String) = UserEntity(
        name = getString("name") ?: "Estudiante",
        role = getString("role") ?: "STUDENT",
        language = getString("language") ?: "ES",
        createdAt = getLong("createdAt") ?: System.currentTimeMillis()
    )

    private fun LevelProgressEntity.toMap() = mapOf(
        "level" to level, "isCompleted" to isCompleted, "bestScore" to bestScore,
        "maxScore" to maxScore, "attempts" to attempts, "lastAttemptAt" to lastAttemptAt
    )

    private fun DocumentSnapshot.toProgress(userId: Long): LevelProgressEntity? {
        val levelName = getString("level") ?: id
        return runCatching {
            LevelProgressEntity(
                userId = userId, level = levelName,
                isCompleted = getBoolean("isCompleted") ?: false,
                bestScore = (getLong("bestScore") ?: 0).toInt(),
                maxScore = (getLong("maxScore") ?: 0).toInt(),
                attempts = (getLong("attempts") ?: 0).toInt(),
                lastAttemptAt = getLong("lastAttemptAt")
            )
        }.getOrNull()
    }

    private fun AttemptEntity.toMap() = mapOf(
        "scenarioId" to scenarioId, "score" to score, "maxScore" to maxScore,
        "isPassed" to isPassed, "startedAt" to startedAt, "completedAt" to completedAt,
        "endNodeId" to endNodeId, "endType" to endType, "decisionsJson" to decisionsJson
    )

    private fun DocumentSnapshot.toAttempt(userId: Long): AttemptEntity? {
        val scenario = getString("scenarioId") ?: return null
        val completed = getLong("completedAt") ?: return null
        return AttemptEntity(
            scenarioId = scenario, userId = userId,
            score = (getLong("score") ?: 0).toInt(), maxScore = (getLong("maxScore") ?: 0).toInt(),
            isPassed = getBoolean("isPassed") ?: false, startedAt = getLong("startedAt") ?: completed,
            completedAt = completed, endNodeId = getString("endNodeId") ?: "",
            endType = getString("endType") ?: "SUCCESS", decisionsJson = getString("decisionsJson") ?: "[]"
        )
    }

    private fun CertificateEntity.toMap() = mapOf(
        "userName" to userName, "level" to level, "scenarioId" to scenarioId,
        "score" to score, "maxScore" to maxScore, "issuedAt" to issuedAt, "signature" to signature
    )

    private fun DocumentSnapshot.toCertificate(userId: Long): CertificateEntity? {
        val level = getString("level") ?: return null
        val scenario = getString("scenarioId") ?: return null
        return CertificateEntity(
            userId = userId, userName = getString("userName") ?: "Estudiante",
            level = level, scenarioId = scenario,
            score = (getLong("score") ?: 0).toInt(), maxScore = (getLong("maxScore") ?: 0).toInt(),
            issuedAt = getLong("issuedAt") ?: System.currentTimeMillis(),
            signature = getString("signature") ?: "", pdfPath = null
        )
    }

    private fun SavedStateEntity.toMap() = mapOf(
        "scenarioId" to scenarioId, "currentNodeId" to currentNodeId, "score" to score,
        "decisionsJson" to decisionsJson, "startTime" to startTime, "savedAt" to savedAt
    )

    private fun DocumentSnapshot.toSavedState(userId: Long): SavedStateEntity? {
        val scenario = getString("scenarioId") ?: return null
        return SavedStateEntity(
            userId = userId, scenarioId = scenario, currentNodeId = getString("currentNodeId") ?: return null,
            score = (getLong("score") ?: 0).toInt(), decisionsJson = getString("decisionsJson") ?: "[]",
            startTime = getLong("startTime") ?: return null, savedAt = getLong("savedAt") ?: System.currentTimeMillis()
        )
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
        addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.cancel() }
    }

    private fun maxOfNullable(a: Long?, b: Long?): Long? = when {
        a == null -> b
        b == null -> a
        else -> maxOf(a, b)
    }

    companion object { private const val KEY_UID = "linked_firebase_uid" }
}
