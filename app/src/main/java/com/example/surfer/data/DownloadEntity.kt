package com.example.surfer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "download_records")
data class DownloadEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val filePath: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val status: DownloadStatus,
    val originalUrl: String
)

enum class DownloadStatus { COMPLETED, IN_PROGRESS, FAILED, CANCELLED }
