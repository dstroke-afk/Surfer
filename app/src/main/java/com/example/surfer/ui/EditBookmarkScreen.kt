package com.example.surfer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.surfer.data.BookmarkEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookmarkScreen(
    url: String,
    viewModel: BrowserViewModel,
    onBack: () -> Unit,
    onSelectFolder: (String) -> Unit
) {
    val bookmark by viewModel.getBookmark(url).collectAsState(initial = null)
    
    var title by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf(url) }
    var folder by remember { mutableStateOf("Mobile Bookmarks") }

    LaunchedEffect(bookmark) {
        bookmark?.let {
            title = it.title
            currentUrl = it.url
            folder = it.folder
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit bookmark") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = currentUrl,
                onValueChange = { currentUrl = it },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth()
            )

            ListItem(
                headlineContent = { Text("Folder") },
                supportingContent = { Text(folder) },
                leadingContent = { Icon(Icons.Rounded.Folder, contentDescription = null) },
                modifier = Modifier.clickable { onSelectFolder(folder) }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.updateBookmark(currentUrl, title, folder)
                    onBack()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Save")
            }
        }
    }
}
