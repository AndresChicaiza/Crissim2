package com.example.crissim.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/** Referencias al archivo separadas de los metadatos firmados del certificado. */
@Entity(
    tableName = "certificate_files",
    primaryKeys = ["certificateUuid"],
    foreignKeys = [
        ForeignKey(
            entity = CertificateEntity::class,
            parentColumns = ["certificateUuid"],
            childColumns = ["certificateUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("certificateUuid")]
)
data class CertificateFileEntity(
    val certificateUuid: String,
    val localUri: String? = null,
    val remoteObjectPath: String? = null,
    val sha256: String? = null,
    val mimeType: String = "application/pdf"
)
