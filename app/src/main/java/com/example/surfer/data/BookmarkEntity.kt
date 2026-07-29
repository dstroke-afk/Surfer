package com.example.surfer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val url: String,
    val title: String,
    val folder: String = "Mobile Bookmarks",
    val timestamp: Long = System.currentTimeMillis()
)
