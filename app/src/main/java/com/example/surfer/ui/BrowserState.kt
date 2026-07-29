package com.example.surfer.ui

import kotlinx.serialization.Serializable

@Serializable
data class BrowserState(
    val tabs: List<BrowserTabState> = listOf(BrowserTabState()),
    val selectedTabIndex: Int = 0
) {
    val currentTab: BrowserTabState? = tabs.getOrNull(selectedTabIndex)
}
