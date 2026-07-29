package com.example.surfer.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.surfer.data.BrowserDatabase
import com.example.surfer.data.BrowserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(private val repository: BrowserRepository) : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    val currentTab: StateFlow<BrowserTabState?> = _state.map { it.currentTab }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserTabState())

    val history = repository.allHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookmarks = repository.allBookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCurrentBookmarked: StateFlow<Boolean> = currentTab
        .flatMapLatest { tab ->
            tab?.let { repository.isBookmarked(it.url) } ?: flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun addNewTab(url: String = "https://www.google.com") {
        _state.update { currentState ->
            val newTabs = currentState.tabs + BrowserTabState(url = url)
            currentState.copy(
                tabs = newTabs,
                selectedTabIndex = newTabs.lastIndex
            )
        }
    }

    fun removeTab(index: Int) {
        _state.update { currentState ->
            if (currentState.tabs.size > 1) {
                val newTabs = currentState.tabs.toMutableList().apply { removeAt(index) }
                val newIndex = if (currentState.selectedTabIndex >= newTabs.size) {
                    newTabs.lastIndex
                } else {
                    currentState.selectedTabIndex
                }
                currentState.copy(
                    tabs = newTabs,
                    selectedTabIndex = newIndex
                )
            } else {
                currentState
            }
        }
    }

    fun selectTab(index: Int) {
        _state.update { currentState ->
            if (index in currentState.tabs.indices) {
                val oldIndex = currentState.selectedTabIndex
                val newTabs = currentState.tabs.toMutableList()
                if (oldIndex != index) {
                    newTabs[oldIndex] = newTabs[oldIndex].copy(isSuspended = true)
                }
                newTabs[index] = newTabs[index].copy(isSuspended = false)
                currentState.copy(
                    tabs = newTabs,
                    selectedTabIndex = index
                )
            } else {
                currentState
            }
        }
    }

    fun updateCurrentTab(update: (BrowserTabState) -> BrowserTabState) {
        _state.update { currentState ->
            val index = currentState.selectedTabIndex
            if (index in currentState.tabs.indices) {
                val newTabs = currentState.tabs.toMutableList()
                newTabs[index] = update(currentState.tabs[index])
                currentState.copy(tabs = newTabs)
            } else {
                currentState
            }
        }
    }

    fun onUrlChange(newUrl: String) {
        updateCurrentTab { it.copy(url = newUrl) }
    }

    fun onLoadingStateChange(isLoading: Boolean) {
        updateCurrentTab { it.copy(isLoading = isLoading) }
    }

    fun onProgressChange(progress: Int) {
        updateCurrentTab { it.copy(progress = progress) }
    }

    fun onNavigationStateChange(canBack: Boolean, canForward: Boolean) {
        updateCurrentTab { it.copy(canGoBack = canBack, canGoForward = canForward) }
    }

    fun onTitleChange(title: String) {
        val tab = currentTab.value
        if (tab != null && title != tab.title && title.isNotBlank() && !tab.url.startsWith("data:")) {
            viewModelScope.launch {
                repository.addHistory(tab.url, title)
            }
        }
        updateCurrentTab { it.copy(title = title) }
    }

    fun toggleBookmark() {
        val tab = currentTab.value ?: return
        viewModelScope.launch {
            if (isCurrentBookmarked.value) {
                repository.removeBookmark(tab.url, tab.title)
            } else {
                repository.addBookmark(tab.url, tab.title)
            }
        }
    }

    fun toggleDesktopSite() {
        updateCurrentTab { it.copy(isDesktopSite = !it.isDesktopSite) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun navigateTo(newUrl: String) {
        var formattedUrl = newUrl.trim()
        if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
            formattedUrl = "https://www.google.com/search?q=$formattedUrl"
        }
        onUrlChange(formattedUrl)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val repository = BrowserRepository(BrowserDatabase.getDatabase(application).browserDao())
                return BrowserViewModel(repository) as T
            }
        }
    }
}
