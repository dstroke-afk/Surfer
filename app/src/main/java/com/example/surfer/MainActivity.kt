package com.example.surfer

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.surfer.navigation.BrowserRoute
import com.example.surfer.navigation.EditBookmarkRoute
import com.example.surfer.navigation.FolderSelectionRoute
import com.example.surfer.ui.*
import com.example.surfer.ui.theme.SurferTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SurferTheme {
                MainApp(intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainApp(intent: Intent?) {
    val backStack = rememberNavBackStack(BrowserRoute)
    val viewModel: BrowserViewModel = viewModel(factory = BrowserViewModel.Factory)
    val context = androidx.compose.ui.platform.LocalContext.current

    DisposableEffect(viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    viewModel.showSnackbar("Download completed")
                }
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(intent) {
        intent?.data?.toString()?.let { url ->
            viewModel.navigateTo(url)
        }
    }
    
    NavDisplay(
        backStack = backStack,
        onBack = { 
            if (backStack.size > 1) {
                backStack.removeAt(backStack.size - 1)
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                is BrowserRoute -> NavEntry<NavKey>(key) {
                    BrowserScreen(
                        viewModel = viewModel,
                        onEditBookmark = { url ->
                            backStack.add(EditBookmarkRoute(url))
                        }
                    )
                }
                is EditBookmarkRoute -> NavEntry<NavKey>(key) {
                    EditBookmarkScreen(
                        url = key.url,
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onSelectFolder = { current ->
                            backStack.add(FolderSelectionRoute(current))
                        }
                    )
                }
                is FolderSelectionRoute -> NavEntry<NavKey>(key) {
                    FolderSelectionScreen(
                        currentFolder = key.currentFolder,
                        viewModel = viewModel,
                        onBack = { backStack.removeLastOrNull() },
                        onFolderSelected = { selected ->
                            backStack.removeLastOrNull()
                        }
                    )
                }
                else -> NavEntry<NavKey>(BrowserRoute) {
                    BrowserScreen(viewModel = viewModel, onEditBookmark = {})
                }
            }
        }
    )
}
