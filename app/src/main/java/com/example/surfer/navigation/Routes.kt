package com.example.surfer.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object BrowserRoute : NavKey

@Serializable
data class EditBookmarkRoute(val url: String) : NavKey

@Serializable
data class FolderSelectionRoute(val currentFolder: String) : NavKey

@Serializable
object DownloadsRoute : NavKey
