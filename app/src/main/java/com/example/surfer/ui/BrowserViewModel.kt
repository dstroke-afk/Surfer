package com.example.surfer.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.surfer.data.BookmarkEntity
import com.example.surfer.data.BrowserDatabase
import com.example.surfer.data.BrowserRepository
import com.example.surfer.data.DownloadEntity
import com.example.surfer.data.DownloadStatus
import com.example.surfer.data.SearchEngine
import com.example.surfer.data.SearchPreferencesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class BrowserViewModel(
    private val repository: BrowserRepository,
    private val searchPreferencesRepository: SearchPreferencesRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    val currentTab: StateFlow<BrowserTabState?> = _state.map { it.currentTab }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserTabState())
    
    // Derived state for groups and top-level tabs
    val groups = _state.map { it.groups }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history = repository.allHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val bookmarks = repository.allBookmarks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloads = repository.allDownloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isCurrentBookmarked: StateFlow<Boolean> = currentTab
        .flatMapLatest { tab ->
            tab?.let { repository.isBookmarked(it.url) } ?: flowOf(false)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun addNewTab(url: String = "https://www.google.com", isIncognito: Boolean = false) {
        _state.update { currentState ->
            val newTabs = currentState.tabs + BrowserTabState(
                url = url, 
                isHomePage = true,
                isIncognito = isIncognito
            )
            currentState.copy(
                tabs = newTabs,
                selectedTabIndex = newTabs.lastIndex
            )
        }
    }

    fun addNewIncognitoTab() {
        addNewTab(isIncognito = true)
    }

    fun removeTab(index: Int) {
        _state.update { currentState ->
            val tabToRemove = currentState.tabs[index]
            val newTabs = currentState.tabs.toMutableList().apply { removeAt(index) }
            
            // If we closed the last tab, add a new default home tab
            if (newTabs.isEmpty()) {
                newTabs.add(BrowserTabState(url = "https://www.google.com", isHomePage = true))
            }
            
            // Check if we just closed the last incognito tab
            val incognitoCount = newTabs.count { it.isIncognito }
            if (tabToRemove.isIncognito && incognitoCount == 0) {
                cleanupIncognito()
            }

            // Cleanup groups if necessary
            val newGroups = if (tabToRemove.groupId != null) {
                currentState.groups.map { group ->
                    if (group.id == tabToRemove.groupId) {
                        group.copy(tabIds = group.tabIds.filter { it != tabToRemove.id })
                    } else group
                }.filter { it.tabIds.isNotEmpty() }
            } else currentState.groups

            val newIndex = if (currentState.selectedTabIndex >= newTabs.size) {
                newTabs.lastIndex
            } else {
                currentState.selectedTabIndex
            }
            currentState.copy(
                tabs = newTabs,
                selectedTabIndex = newIndex,
                groups = newGroups
            )
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

    fun captureSnapshot(webView: WebView) {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        webView.draw(canvas)
        
        // Scale down for thumbnail
        val thumbnail = Bitmap.createScaledBitmap(bitmap, width / 4, height / 4, true)
        
        updateCurrentTab { it.copy(thumbnail = thumbnail) }
    }

    fun mergeTabs(draggedTabId: String, targetId: String) {
        _state.update { currentState ->
            val draggedTab = currentState.tabs.find { it.id == draggedTabId } ?: return@update currentState
            if (draggedTab.id == targetId) return@update currentState

            val newGroups = currentState.groups.toMutableList()
            val newTabs = currentState.tabs.map { it.copy() }.toMutableList()

            // Remove from old group if any
            if (draggedTab.groupId != null) {
                val oldGroupIndex = newGroups.indexOfFirst { it.id == draggedTab.groupId }
                if (oldGroupIndex != -1) {
                    newGroups[oldGroupIndex] = newGroups[oldGroupIndex].copy(
                        tabIds = newGroups[oldGroupIndex].tabIds.filter { it != draggedTab.id }
                    )
                    if (newGroups[oldGroupIndex].tabIds.isEmpty()) {
                        newGroups.removeAt(oldGroupIndex)
                    }
                }
            }

            // Check if target is a group
            val targetGroup = currentState.groups.find { it.id == targetId }
            
            val finalGroupId: String
            if (targetGroup != null) {
                // Merging into an existing group card
                finalGroupId = targetId
                val groupIndex = newGroups.indexOfFirst { it.id == targetId }
                newGroups[groupIndex] = newGroups[groupIndex].copy(
                    tabIds = (newGroups[groupIndex].tabIds + draggedTab.id).distinct()
                )
            } else {
                // Check if target is a tab card
                val targetTab = currentState.tabs.find { it.id == targetId } ?: return@update currentState
                
                val targetGroupId = targetTab.groupId
                finalGroupId = targetGroupId ?: UUID.randomUUID().toString()

                if (targetGroupId == null) {
                    // Create new group from two tabs
                    newGroups.add(TabGroup(finalGroupId, "New Group", listOf(targetTab.id, draggedTab.id)))
                    // Update target tab's group reference
                    val targetIndex = newTabs.indexOfFirst { it.id == targetTab.id }
                    newTabs[targetIndex] = newTabs[targetIndex].copy(groupId = finalGroupId)
                } else {
                    // Merging into a tab that is already in a group
                    val groupIndex = newGroups.indexOfFirst { it.id == targetGroupId }
                    if (groupIndex != -1) {
                        newGroups[groupIndex] = newGroups[groupIndex].copy(
                            tabIds = (newGroups[groupIndex].tabIds + draggedTab.id).distinct()
                        )
                    }
                }
            }

            // Update dragged tab's group reference
            val draggedIndex = newTabs.indexOfFirst { it.id == draggedTab.id }
            newTabs[draggedIndex] = newTabs[draggedIndex].copy(groupId = finalGroupId)

            currentState.copy(tabs = newTabs, groups = newGroups)
        }
    }

    fun renameGroup(groupId: String, newName: String) {
        _state.update { currentState ->
            val newGroups = currentState.groups.map { 
                if (it.id == groupId) it.copy(name = newName) else it
            }
            currentState.copy(groups = newGroups)
        }
    }

    fun onUrlChange(newUrl: String) {
        updateCurrentTab { it.copy(url = newUrl, isHomePage = false) }
    }

    fun goHome() {
        updateCurrentTab { it.copy(url = "https://www.google.com", isHomePage = true) }
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
        if (tab != null && title != tab.title && title.isNotBlank() && !tab.url.startsWith("data:") && !tab.isIncognito) {
            viewModelScope.launch {
                repository.addHistory(tab.url, title)
            }
        }
        updateCurrentTab { it.copy(title = title) }
    }

    fun updatePageInfo(webView: WebView) {
        val url = webView.url ?: return
        val isSecure = url.startsWith("https")
        
        val js = "(function() { " +
                "const cookies = document.cookie ? document.cookie.split(';').length : 0; " +
                "if (navigator.storage && navigator.storage.estimate) { " +
                "  return navigator.storage.estimate().then(est => JSON.stringify({cookies: cookies, storage: est.usage || 0})); " +
                "} else { " +
                "  return JSON.stringify({cookies: cookies, storage: 0}); " +
                "} " +
                "})()"
        
        webView.evaluateJavascript(js) { result ->
            try {
                val cleanedJson = if (result != null && result.startsWith("\"") && result.endsWith("\"")) {
                    result.substring(1, result.length - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\")
                } else result ?: "{}"
                
                val json = org.json.JSONObject(cleanedJson)
                val cookies = json.optInt("cookies", 0)
                val storage = json.optLong("storage", 0)
                
                updateCurrentTab { it.copy(
                    isSecure = isSecure,
                    cookieCount = cookies,
                    storageUsage = storage
                ) }
            } catch (e: Exception) {
                updateCurrentTab { it.copy(isSecure = isSecure) }
            }
        }
        
        viewModelScope.launch {
            repository.getLastVisit(url).take(1).collect { lastVisit ->
                updateCurrentTab { it.copy(lastVisitedTime = lastVisit?.timestamp) }
            }
        }
    }

    fun getRelativeTime(timestamp: Long?): String {
        if (timestamp == null) return "First time visiting this site"
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 0 -> "Last visited recently"
            diff < 60000 -> "Last visited just now"
            diff < 3600000 -> "Last visited ${diff / 60000}m ago"
            diff < 86400000 -> "Last visited today"
            diff < 172800000 -> "Last visited yesterday"
            else -> {
                val date = java.util.Date(timestamp)
                val format = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
                "Last visited ${format.format(date)}"
            }
        }
    }

    private val _lastUsedFolder = MutableStateFlow("Mobile Bookmarks")
    val lastUsedFolder: StateFlow<String> = _lastUsedFolder.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun toggleBookmark() {
        val tab = currentTab.value ?: return
        viewModelScope.launch {
            if (isCurrentBookmarked.value) {
                repository.removeBookmark(tab.url, tab.title)
            } else {
                repository.addBookmark(tab.url, tab.title, _lastUsedFolder.value)
                _snackbarMessage.value = "Bookmark saved to ${_lastUsedFolder.value}"
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun updateBookmark(url: String, title: String, folder: String) {
        viewModelScope.launch {
            repository.addBookmark(url, title, folder)
            _lastUsedFolder.value = folder
        }
    }

    fun getBookmark(url: String): Flow<BookmarkEntity?> = repository.getBookmarkByUrl(url)

    fun getAllFolders(): Flow<List<String>> = combine(
        repository.getAllFolders(),
        repository.getCreatedFolders()
    ) { existing, created ->
        (existing + created + "Mobile Bookmarks" + "Reading List").distinct().sorted()
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name)
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun saveHtml(title: String, html: String, context: android.content.Context) {
        val tab = currentTab.value ?: return
        viewModelScope.launch {
            val fileName = "${title.replace(Regex("[^a-zA-Z0-9]"), "_")}.html"
            var finalPath = ""
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/html")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }

                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

                if (uri != null) {
                    try {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputBuffer(html, outputStream)
                        }
                        finalPath = uri.toString()
                    } catch (e: Exception) {
                        // Silently fail or log as requested to remove the popup
                    }
                }
            } else {
                // Fallback for older Android versions
                try {
                    val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val file = java.io.File(downloadsDir, fileName)
                    java.io.FileOutputStream(file).use { outputStream ->
                        outputBuffer(html, outputStream)
                    }
                    finalPath = file.absolutePath
                } catch (e: Exception) {
                    // Silently fail or log
                }
            }

            if (finalPath.isNotEmpty()) {
                repository.addDownload(
                    DownloadEntity(
                        fileName = fileName,
                        filePath = finalPath,
                        mimeType = "text/html",
                        totalBytes = html.length.toLong(),
                        downloadedBytes = html.length.toLong(),
                        status = DownloadStatus.COMPLETED,
                        originalUrl = tab.url
                    )
                )
            }
        }
    }

    fun deleteDownload(id: String) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun onDownloadStart(
        url: String,
        fileName: String,
        filePath: String,
        mimeType: String?,
        contentLength: Long
    ) {
        viewModelScope.launch {
            repository.addDownload(
                DownloadEntity(
                    fileName = fileName,
                    filePath = filePath,
                    mimeType = mimeType,
                    totalBytes = contentLength,
                    downloadedBytes = 0, // In reality, we'd track this via DownloadManager, but for simplicity we start at 0
                    status = DownloadStatus.IN_PROGRESS,
                    originalUrl = url
                )
            )
        }
    }


    private fun outputBuffer(content: String, outputStream: java.io.OutputStream) {
        val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream))
        writer.write(content)
        writer.flush()
    }

    fun toggleDesktopSite() {
        updateCurrentTab { it.copy(isDesktopSite = !it.isDesktopSite) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private val _isEnginePickerExpanded = MutableStateFlow(false)
    val isEnginePickerExpanded = _isEnginePickerExpanded.asStateFlow()

    val selectedSearchEngine: StateFlow<SearchEngine> = searchPreferencesRepository.selectedSearchEngine
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchEngine.GOOGLE)

    fun setEnginePickerExpanded(expanded: Boolean) {
        _isEnginePickerExpanded.value = expanded
    }

    fun selectSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            searchPreferencesRepository.saveSearchEngine(engine)
            _isEnginePickerExpanded.value = false
        }
    }

    fun navigateTo(newUrl: String) {
        val trimmed = newUrl.trim()
        if (trimmed.isEmpty()) return

        val isUrl = trimmed.contains(".") && !trimmed.contains(" ") ||
                trimmed.startsWith("http://") ||
                trimmed.startsWith("https://") ||
                trimmed.startsWith("about:") ||
                trimmed.startsWith("chrome:") ||
                trimmed.startsWith("file://")

        val formattedUrl = if (isUrl) {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.contains(":")) {
                "https://$trimmed"
            } else {
                trimmed
            }
        } else {
            "${selectedSearchEngine.value.searchUrl}$trimmed"
        }
        onUrlChange(formattedUrl)
    }

    private fun cleanupIncognito() {
        android.webkit.CookieManager.getInstance().flush()
        // Note: Clearing cookies/cache here might affect other tabs since CookieManager is global.
        // A better approach for isolated incognito would be multiple profiles (Android 11+),
        // but for now we follow the instruction to clear data.
        android.webkit.WebStorage.getInstance().deleteAllData()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = BrowserDatabase.getDatabase(application)
                val repository = BrowserRepository(database.browserDao())
                val searchPreferencesRepository = SearchPreferencesRepository(application)
                return BrowserViewModel(repository, searchPreferencesRepository) as T
            }
        }
    }
}
