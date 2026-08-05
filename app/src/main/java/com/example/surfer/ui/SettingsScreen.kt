package com.example.surfer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ViewHeadline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.surfer.data.AddressBarPosition
import com.example.surfer.data.SearchEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val selectedSearchEngine by viewModel.selectedSearchEngine.collectAsStateWithLifecycle()
    val addressBarPosition by viewModel.addressBarPosition.collectAsStateWithLifecycle()

    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showAddressBarPositionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ListItem(
                headlineContent = { Text("Search Engine") },
                supportingContent = { Text(selectedSearchEngine.displayName) },
                leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
                modifier = Modifier.clickable { showSearchEngineDialog = true }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Address Bar Position") },
                supportingContent = { Text(if (addressBarPosition == AddressBarPosition.TOP) "Top" else "Bottom") },
                leadingContent = { Icon(Icons.Default.ViewHeadline, contentDescription = null) },
                modifier = Modifier.clickable { showAddressBarPositionDialog = true }
            )
        }
    }

    if (showSearchEngineDialog) {
        AlertDialog(
            onDismissRequest = { showSearchEngineDialog = false },
            title = { Text("Search Engine") },
            text = {
                Column {
                    SearchEngine.entries.forEach { engine ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSearchEngine(engine)
                                    showSearchEngineDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = engine == selectedSearchEngine,
                                onClick = {
                                    viewModel.selectSearchEngine(engine)
                                    showSearchEngineDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(engine.displayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchEngineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddressBarPositionDialog) {
        AlertDialog(
            onDismissRequest = { showAddressBarPositionDialog = false },
            title = { Text("Address Bar Position") },
            text = {
                Column {
                    AddressBarPosition.entries.forEach { position ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAddressBarPosition(position)
                                    showAddressBarPositionDialog = false
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = position == addressBarPosition,
                                onClick = {
                                    viewModel.setAddressBarPosition(position)
                                    showAddressBarPositionDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (position == AddressBarPosition.TOP) "Top" else "Bottom")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddressBarPositionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
