package com.example.surfer.ui

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.surfer.data.DownloadEntity
import com.example.surfer.data.DownloadStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val downloads by viewModel.downloads.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No downloads yet", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads) { download ->
                    DownloadItem(
                        download = download,
                        onClick = { openFile(context, download) },
                        onShowInFolder = { openFolder(context, download) },
                        onDelete = { viewModel.deleteDownload(download.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DownloadItem(
    download: DownloadEntity,
    onClick: () -> Unit,
    onShowInFolder: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = getFileIcon(download.fileName, download.mimeType),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatSize(download.totalBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(download.timestamp),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = download.originalUrl,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                TextButton(onClick = onShowInFolder) {
                    Text("Show in folder")
                }
            }
            
            if (download.status == DownloadStatus.IN_PROGRESS) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = if (download.totalBytes > 0) download.downloadedBytes.toFloat() / download.totalBytes else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (download.status == DownloadStatus.FAILED) {
                Text(
                    text = "Failed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun getFileIcon(fileName: String, mimeType: String?): ImageVector {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    return when {
        extension == "pdf" || mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
        listOf("jpg", "jpeg", "png", "gif", "webp").contains(extension) || mimeType?.startsWith("image/") == true -> Icons.Rounded.Image
        extension == "apk" || mimeType == "application/vnd.android.package-archive" -> Icons.Rounded.Android
        listOf("mp3", "wav", "ogg").contains(extension) || mimeType?.startsWith("audio/") == true -> Icons.Rounded.AudioFile
        listOf("mp4", "mkv", "avi").contains(extension) || mimeType?.startsWith("video/") == true -> Icons.Rounded.VideoFile
        else -> Icons.Rounded.Description
    }
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun openFile(context: Context, download: DownloadEntity) {
    try {
        val uri = if (download.filePath.startsWith("content://")) {
            Uri.parse(download.filePath)
        } else {
            val file = File(download.filePath)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, download.mimeType ?: context.contentResolver.getType(uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot open file", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun openFolder(context: Context, download: DownloadEntity) {
    try {
        val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            context.startActivity(intent)
        } catch (e2: Exception) {
            android.widget.Toast.makeText(context, "Cannot open folder", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
