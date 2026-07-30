package com.example.surfer.ui

import android.graphics.Bitmap
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
    val isHomePage: Boolean = true,
    val groupId: String? = null,
    val isSecure: Boolean? = null,
    val cookieCount: Int = 0,
    val storageUsage: Long = 0,
    val lastVisitedTime: Long? = null,
    val isIncognito: Boolean = false,
    @Transient val thumbnail: Bitmap? = null
)
