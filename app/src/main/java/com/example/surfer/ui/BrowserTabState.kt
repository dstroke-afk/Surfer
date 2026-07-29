package com.example.surfer.ui

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class BrowserTabState(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "https://www.google.com",
    val title: String = "New Tab",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val isDesktopSite: Boolean = false,
    val isSuspended: Boolean = false,
    val isHomePage: Boolean = true
)
