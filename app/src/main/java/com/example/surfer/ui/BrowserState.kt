package com.example.surfer.ui

import kotlinx.serialization.Serializable

@Serializable
data class TabGroup(
    val id: String,
    val name: String,
    val tabIds: List<String>
)

@Serializable
data class BrowserState(
    val tabs: List<BrowserTabState> = listOf(BrowserTabState()),
    val selectedTabIndex: Int = 0,
    val groups: List<TabGroup> = emptyList()
) {
    val currentTab: BrowserTabState? = tabs.getOrNull(selectedTabIndex)
}
