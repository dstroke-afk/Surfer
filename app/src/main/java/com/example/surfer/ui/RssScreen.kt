package com.example.surfer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.surfer.data.RssItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RssScreen(
    viewModel: RssViewModel = viewModel(),
    onItemClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val feeds by viewModel.feeds.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedFeedName by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFeedName ?: "RSS Feeds") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedFeedName != null) {
                            selectedFeedName = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (selectedFeedName == null) {
                FeedList(
                    predefinedFeeds = viewModel.predefinedFeeds,
                    onFeedClick = { name, url ->
                        selectedFeedName = name
                        if (!feeds.containsKey(name)) {
                            viewModel.fetchFeed(name, url)
                        }
                    }
                )
            } else {
                val feed = feeds[selectedFeedName!!]
                if (isLoading && feed == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (feed != null) {
                    RssItemList(items = feed.items, onItemClick = onItemClick)
                }
            }
        }
    }
}

@Composable
fun FeedList(
    predefinedFeeds: List<Pair<String, String>>,
    onFeedClick: (String, String) -> Unit
) {
    LazyColumn {
        items(predefinedFeeds) { (name, url) ->
            ListItem(
                headlineContent = { Text(name) },
                supportingContent = { Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Icon(Icons.Default.RssFeed, contentDescription = null) },
                modifier = Modifier.clickable { onFeedClick(name, url) }
            )
        }
    }
}

@Composable
fun RssItemList(
    items: List<RssItem>,
    onItemClick: (String) -> Unit
) {
    LazyColumn {
        items(items) { item ->
            RssItemRow(item = item, onClick = { onItemClick(item.link) })
        }
    }
}

@Composable
fun RssItemRow(
    item: RssItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (item.thumbnailUrl != null) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.pubDate != null) {
                    Text(
                        text = item.pubDate,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
