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
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.ui.zIndex
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
    onEditBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val browserState by viewModel.state.collectAsState()
    val tabs = browserState.tabs
    val selectedTabIndex = browserState.selectedTabIndex
    val currentTab = browserState.currentTab

    val isBookmarked by viewModel.isCurrentBookmarked.collectAsState()
    val history by viewModel.history.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Edit",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                onEditBookmark(url)
            }
            viewModel.clearSnackbar()
        }
    }

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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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

                                // Menu button (Restored to top right)
                                Box {
                                    IconButton(onClick = { showMenu = true }) {
                                        Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                        modifier = Modifier.width(280.dp)
                                    ) {
                                        // Top Row of Icons
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { webView?.goBack(); showMenu = false }, enabled = canGoBack) {
                                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                                            }
                                            IconButton(onClick = { viewModel.toggleBookmark(); showMenu = false }) {
                                                Icon(
                                                    if (isBookmarked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                    contentDescription = "Bookmark",
                                                    tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(onClick = { /* Download trigger */ showMenu = false }) {
                                                Icon(Icons.Rounded.Download, contentDescription = "Download")
                                            }
                                            IconButton(onClick = { /* Info trigger */ showMenu = false }) {
                                                Icon(Icons.Rounded.Info, contentDescription = "Info")
                                            }
                                            IconButton(onClick = { webView?.reload(); showMenu = false }) {
                                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                                            }
                                        }
                                        
                                        HorizontalDivider()
                                        
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
                                }
                            }
                        },
                        actions = {
                            // Empty actions as we handle everything in the title row
                        }
                    )
                    
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
                            },
                            onCaptureSnapshot = { viewModel.captureSnapshot(it) }
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
            val groups by viewModel.groups.collectAsState()
            TabSwitcherContent(
                tabs = tabs,
                groups = groups,
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
                },
                onMergeTabs = { dragged, target ->
                    viewModel.mergeTabs(dragged, target)
                },
                onRenameGroup = { id, name ->
                    viewModel.renameGroup(id, name)
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
    groups: List<TabGroup>,
    selectedTabIndex: Int,
    onTabSelect: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onNewTab: () -> Unit,
    onMergeTabs: (String, String) -> Unit,
    onRenameGroup: (String, String) -> Unit
) {
    var draggedTabId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedGroupForDetail by remember { mutableStateOf<TabGroup?>(null) }

    // Map tab IDs to their current positions in the grid for drag detection
    val itemPositions = remember { mutableStateMapOf<String, Pair<Offset, IntSize>>() }

    val topLevelItems = remember(tabs, groups) {
        val groupedTabIds = groups.flatMap { it.tabIds }.toSet()
        val ungroupedTabs = tabs.filter { it.id !in groupedTabIds }
        (ungroupedTabs.map { it to null } + groups.map { null to it }).sortedBy { 
            val id = it.first?.id ?: it.second?.id ?: ""
            // Sort by earliest tab in item
            val firstTabId = it.first?.id ?: it.second?.tabIds?.firstOrNull() ?: ""
            tabs.indexOfFirst { t -> t.id == firstTabId }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                items(topLevelItems) { item ->
                    val tab = item.first
                    val group = item.second
                    val itemId = tab?.id ?: group?.id ?: ""

                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                itemPositions[itemId] = coordinates.positionInWindow() to coordinates.size
                            }
                            .pointerInput(itemId) {
                                if (tab != null) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { offset ->
                                            draggedTabId = tab.id
                                            dragOffset = Offset.Zero
                                        },
                                        onDragEnd = {
                                            draggedTabId?.let { draggedId ->
                                                val absoluteDragPos = itemPositions[draggedId]?.first?.plus(dragOffset) ?: Offset.Zero
                                                
                                                // Check for overlap with other items
                                                itemPositions.forEach { (targetId, posAndSize) ->
                                                    if (targetId != draggedId) {
                                                        val (pos, size) = posAndSize
                                                        if (absoluteDragPos.x > pos.x && absoluteDragPos.x < pos.x + size.width &&
                                                            absoluteDragPos.y > pos.y && absoluteDragPos.y < pos.y + size.height) {
                                                            onMergeTabs(draggedId, targetId)
                                                        }
                                                    }
                                                }
                                            }
                                            draggedTabId = null
                                        },
                                        onDragCancel = { draggedTabId = null },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                        }
                                    )
                                }
                            }
                            .alpha(if (draggedTabId == itemId) 0.5f else 1.0f)
                    ) {
                        if (tab != null) {
                            TabCard(
                                tab = tab,
                                isSelected = tabs.indexOf(tab) == selectedTabIndex,
                                onSelect = { onTabSelect(tabs.indexOf(tab)) },
                                onClose = { onTabClose(tabs.indexOf(tab)) }
                            )
                        } else if (group != null) {
                            GroupCard(
                                group = group,
                                tabs = tabs,
                                onClick = { selectedGroupForDetail = group }
                            )
                        }
                    }
                }
            }
        }

        // Dragging overlay
        draggedTabId?.let { id ->
            val draggedTab = tabs.find { it.id == id }
            if (draggedTab != null) {
                val startPos = itemPositions[id]?.first ?: Offset.Zero
                Box(
                    modifier = Modifier
                        .offset(
                            x = (startPos.x + dragOffset.x).pxToDp(),
                            y = (startPos.y + dragOffset.y).pxToDp()
                        )
                        .size(150.dp) // Approximate card size
                        .zIndex(100f)
                ) {
                    TabCard(tab = draggedTab, isSelected = false, onSelect = {}, onClose = {})
                }
            }
        }
        
        selectedGroupForDetail?.let { group ->
            Popup(
                onDismissRequest = { selectedGroupForDetail = null },
                properties = PopupProperties(focusable = true)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        var groupName by remember { mutableStateOf(group.name) }
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { 
                                groupName = it
                                onRenameGroup(group.id, it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.titleMedium,
                            label = { Text("Group Name") }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val groupTabs = tabs.filter { it.id in group.tabIds }
                            itemsIndexed(groupTabs) { _, groupTab ->
                                TabCard(
                                    tab = groupTab,
                                    isSelected = tabs.indexOf(groupTab) == selectedTabIndex,
                                    onSelect = { 
                                        onTabSelect(tabs.indexOf(groupTab))
                                        selectedGroupForDetail = null
                                    },
                                    onClose = { onTabClose(tabs.indexOf(groupTab)) }
                                )
                            }
                        }
                        
                        Button(
                            onClick = { selectedGroupForDetail = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabCard(
    tab: BrowserTabState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.medium
            )
            .clickable { onSelect() },
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
                    onClick = onClose,
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
                if (tab.thumbnail != null) {
                    Image(
                        bitmap = tab.thumbnail.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small)
                    )
                } else {
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

@Composable
fun GroupCard(
    group: TabGroup,
    tabs: List<BrowserTabState>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
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
                // Stack of thumbnails
                val groupTabs = tabs.filter { it.id in group.tabIds }.take(4)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    userScrollEnabled = false
                ) {
                    items(groupTabs) { gTab ->
                        if (gTab.thumbnail != null) {
                            Image(
                                bitmap = gTab.thumbnail.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.extraSmall)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
                        }
                    }
                }
            }
            
            Text(
                text = "${group.tabIds.size} tabs",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun Offset.pxToDp() = with(androidx.compose.ui.platform.LocalDensity.current) { this@pxToDp.y.toDp() }
// Simple helper - actually needs x and y
@Composable
fun Float.pxToDp() = with(androidx.compose.ui.platform.LocalDensity.current) { this@pxToDp.toDp() }

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
                        innerTextField()
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
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
    onSslError: (SslErrorHandler, SslError) -> Unit,
    onCaptureSnapshot: (WebView) -> Unit
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
                        view?.let { onCaptureSnapshot(it) }
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
