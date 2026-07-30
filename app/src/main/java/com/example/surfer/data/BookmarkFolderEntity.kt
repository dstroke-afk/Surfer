package com.example.surfer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey val name: String,
    val timestamp: Long = System.currentTimeMillis()
)
