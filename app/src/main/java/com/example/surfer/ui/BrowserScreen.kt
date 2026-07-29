package com.example.surfer.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.net.http.SslError
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import androidx.compose.material.icons.rounded.Warning
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowWidthSizeClass
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.surfer.ui.theme.SurferTheme

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp,navigation=buttons")
@Composable
fun BrowserScreenPreview() {
    // Note: This preview won't fully work without a real repository, but we can't easily mock it here.
    // For now, we'll keep it as a placeholder or remove it if it blocks the build.
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val browserState by viewModel.state.collectAsState()
    val tabs = browserState.tabs
    val selectedTabIndex = browserState.selectedTabIndex
    val currentTab = browserState.currentTab

    val isBookmarked by viewModel.isCurrentBookmarked.collectAsState()
    val history by viewModel.history.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    if (currentTab == null) return

    val url = currentTab.url
    val isLoading = currentTab.isLoading
    val progress = currentTab.progress
    val canGoBack = currentTab.canGoBack

    var textFieldValue by remember(url) { mutableStateOf(url) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showHistoryBookmarks by remember { mutableStateOf(false) }
    var showTabSwitcher by remember { mutableStateOf(false) }
    var sslErrorToHandle by remember { mutableStateOf<Pair<SslErrorHandler, SslError>?>(null) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    NavigationSuiteScaffold(
        layoutType = if (isExpanded) NavigationSuiteType.NavigationRail else NavigationSuiteType.None,
        navigationSuiteItems = {
            if (isExpanded) {
                // Defensive guard for index and size
                val safeIndex = if (tabs.isNotEmpty()) selectedTabIndex.coerceAtMost(tabs.size - 1) else 0
                
                tabs.forEachIndexed { index, tab ->
                    item(
                        selected = safeIndex == index,
                        onClick = { viewModel.selectTab(index) },
                        icon = {
                            Icon(
                                if (safeIndex == index) Icons.Rounded.Tab else Icons.Rounded.TabUnselected,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                item(
                    selected = false,
                    onClick = { viewModel.addNewTab() },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "New Tab") },
                    label = { Text("New Tab") }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.statusBarsPadding()) {
                    TopAppBar(
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Home button
                                IconButton(onClick = { viewModel.goHome() }) {
                                    Icon(Icons.Rounded.Home, contentDescription = "Home")
                                }

                                // Pill-shaped Omnibox
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        BasicTextField(
                                            value = textFieldValue,
                                            onValueChange = { textFieldValue = it },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp)
                                                .horizontalScroll(rememberScrollState()),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                            keyboardActions = KeyboardActions(
                                                onGo = {
                                                    viewModel.navigateTo(textFieldValue)
                                                    focusManager.clearFocus()
                                                }
                                            ),
                                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                            decorationBox = { innerTextField ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Icon(
                                                        if (url.startsWith("https")) Icons.Rounded.Lock else Icons.Rounded.Info,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        if (textFieldValue.isEmpty()) {
                                                            Text(
                                                                "Search or type URL",
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                    if (isLoading) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(16.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }

                                // New Tab button
                                IconButton(onClick = { viewModel.addNewTab() }) {
                                    Icon(Icons.Rounded.Add, contentDescription = "New Tab")
                                }

                                // Tab Count button
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable { showTabSwitcher = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .border(1.5.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.extraSmall),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabs.size.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Menu button
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
                                }
                            }
                        },
                        actions = {
                            // Empty actions as we handle everything in the title row
                        }
                    )
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isBookmarked) "Remove Bookmark" else "Bookmark") },
                            onClick = {
                                viewModel.toggleBookmark()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("History & Bookmarks") },
                            onClick = {
                                showHistoryBookmarks = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.History, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Desktop Site") },
                            onClick = {
                                viewModel.toggleDesktopSite()
                                showMenu = false
                            },
                            trailingIcon = {
                                Checkbox(
                                    checked = currentTab.isDesktopSite,
                                    onCheckedChange = null
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Refresh") },
                            onClick = {
                                webView?.reload()
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) }
                        )
                    }

                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (currentTab.isHomePage) {
                    HomePage(
                        onSearch = { viewModel.navigateTo(it) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    key(currentTab.id) {
                        WebViewContainer(
                            url = currentTab.url,
                            isDesktopSite = currentTab.isDesktopSite,
                            isSuspended = currentTab.isSuspended,
                            onWebViewCreated = { webView = it },
                            onProgressChanged = { viewModel.onProgressChange(it) },
                            onLoadingStateChanged = { viewModel.onLoadingStateChange(it) },
                            onNavigationStateChanged = { canBack, canForward ->
                                viewModel.onNavigationStateChange(canBack, canForward)
                            },
                            onUrlChanged = { viewModel.onUrlChange(it) },
                            onTitleChanged = { viewModel.onTitleChange(it) },
                            onSslError = { handler, error ->
                                sslErrorToHandle = handler to error
                            }
                        )
                    }
                }
            }
        }
    }

    if (showHistoryBookmarks) {
        ModalBottomSheet(
            onDismissRequest = { showHistoryBookmarks = false },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            HistoryBookmarksContent(
                history = history,
                bookmarks = bookmarks,
                onUrlClick = {
                    viewModel.navigateTo(it)
                    showHistoryBookmarks = false
                },
                onClearHistory = { viewModel.clearHistory() }
            )
        }
    }

    if (showTabSwitcher) {
        ModalBottomSheet(
            onDismissRequest = { showTabSwitcher = false },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            TabSwitcherContent(
                tabs = tabs,
                selectedTabIndex = selectedTabIndex,
                onTabSelect = { index ->
                    viewModel.selectTab(index)
                    showTabSwitcher = false
                },
                onTabClose = { index ->
                    viewModel.removeTab(index)
                },
                onNewTab = {
                    viewModel.addNewTab()
                    showTabSwitcher = false
                }
            )
        }
    }

    sslErrorToHandle?.let { (handler, error) ->
        AlertDialog(
            onDismissRequest = { 
                handler.cancel()
                sslErrorToHandle = null 
            },
            title = { Text("SSL Error") },
            text = { Text("There is a problem with the security certificate for this site: ${error.url}\n\nDo you want to continue anyway?") },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = {
                TextButton(onClick = {
                    handler.proceed()
                    sslErrorToHandle = null
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    handler.cancel()
                    sslErrorToHandle = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TabSwitcherContent(
    tabs: List<BrowserTabState>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onNewTab: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${tabs.size} Tabs",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onNewTab) {
                Icon(Icons.Rounded.Add, contentDescription = "New Tab")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .border(
                            width = if (index == selectedTabIndex) 2.dp else 0.dp,
                            color = if (index == selectedTabIndex) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onTabSelect(index) },
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onTabClose(index) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close Tab",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = tab.url,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Mini window placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Web,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomePage(
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        // Google-style Logo
        Text(
            text = "Google",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Large Search Box
        var searchText by remember { mutableStateOf("") }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(searchText) }),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (searchText.isEmpty()) {
                            Text(
                                "Search Google or type URL",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    }
                )
                IconButton(onClick = { /* Voice placeholder */ }) {
                    Icon(Icons.Rounded.Mic, contentDescription = "Voice Search")
                }
                IconButton(onClick = { /* Lens placeholder */ }) {
                    Icon(Icons.Rounded.CenterFocusWeak, contentDescription = "Google Lens")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // AI Mode & Incognito Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { /* AI Mode */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Mode", color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Button(
                onClick = { /* Incognito */ },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Incognito", color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Shortcuts Grid (Renamed from Frequently Visited)
        val shortcuts = listOf(
            "Google" to "https://www.google.com",
            "YouTube" to "https://www.youtube.com",
            "Amazon" to "https://www.amazon.com",
            "Wikipedia" to "https://www.wikipedia.org"
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false
        ) {
            items(shortcuts) { (name, url) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onSearch(url) }.padding(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                when(name) {
                                    "Google" -> Icons.Rounded.Search
                                    "YouTube" -> Icons.Rounded.PlayArrow
                                    "Amazon" -> Icons.Rounded.ShoppingBag
                                    else -> Icons.Rounded.Language
                                },
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun HistoryBookmarksContent(
    history: List<com.example.surfer.data.HistoryEntity>,
    bookmarks: List<com.example.surfer.data.BookmarkEntity>,
    onUrlClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    Column {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bookmarks") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("History") })
        }
        if (selectedTab == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(bookmarks) { bookmark ->
                    ListItem(
                        headlineContent = { Text(bookmark.title, maxLines = 1) },
                        supportingContent = { Text(bookmark.url, maxLines = 1) },
                        modifier = Modifier.clickable { onUrlClick(bookmark.url) }
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Button(
                    onClick = onClearHistory,
                    modifier = Modifier.padding(8.dp).align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear History")
                }
                LazyColumn {
                    items(history) { item ->
                        ListItem(
                            headlineContent = { Text(item.title, maxLines = 1) },
                            supportingContent = { Text(item.url, maxLines = 1) },
                            modifier = Modifier.clickable { onUrlClick(item.url) }
                        )
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    url: String,
    isDesktopSite: Boolean,
    isSuspended: Boolean,
    onWebViewCreated: (WebView) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onLoadingStateChanged: (Boolean) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onUrlChanged: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onSslError: (SslErrorHandler, SslError) -> Unit
) {
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    val mobileUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    if (isSuspended) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Tab Suspended", style = MaterialTheme.typography.titleMedium)
                Text("Tap to reload", style = MaterialTheme.typography.bodySmall)
            }
        }
        return
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    allowFileAccess = true
                }
                
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingStateChanged(true)
                        url?.let { onUrlChanged(it) }
                        onNavigationStateChanged(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingStateChanged(false)
                        onNavigationStateChanged(view?.canGoBack() ?: false, view?.canGoForward() ?: false)
                        view?.title?.let { onTitleChanged(it) }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        if (handler != null && error != null) {
                            onSslError(handler, error)
                        } else {
                            super.onReceivedSslError(view, handler, error)
                        }
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChanged(newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChanged(it) }
                    }

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.grant(request.resources)
                    }
                }

                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                    val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl))
                    request.setMimeType(mimetype)
                    request.addRequestHeader("User-Agent", userAgent)
                    request.setDescription("Downloading file...")
                    request.setTitle(android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype))
                    request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype))
                    
                    val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                    dm.enqueue(request)
                    android.widget.Toast.makeText(context, "Download started", android.widget.Toast.LENGTH_SHORT).show()
                }

                onWebViewCreated(this)
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.settings.userAgentString = if (isDesktopSite) desktopUserAgent else mobileUserAgent
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
