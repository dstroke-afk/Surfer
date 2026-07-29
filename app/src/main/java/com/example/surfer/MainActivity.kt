package com.example.surfer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.surfer.navigation.BrowserRoute
import com.example.surfer.ui.BrowserScreen
import com.example.surfer.ui.BrowserViewModel
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
                is BrowserRoute -> NavEntry(key) {
                    BrowserScreen(viewModel = viewModel)
                }
                else -> NavEntry(key) {
                    // Fallback
                }
            }
        }
    )
}
