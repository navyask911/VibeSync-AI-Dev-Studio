package com.vibesync.promptstudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibesync.promptstudio.presentation.PromptStudioScreen
import com.vibesync.promptstudio.presentation.PromptStudioViewModel
import com.vibesync.promptstudio.presentation.SettingsScreen
import com.vibesync.promptstudio.presentation.SettingsViewModel
import com.vibesync.promptstudio.presentation.theme.VibeSyncTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VibeSyncTheme {
                AppNavigation(
                    navController = rememberNavController(),
                    onConnectOAuth = {
                        lifecycleScope.launch {
                            runCatching { (application as VibeSyncApplication).container.oauthManager.launch(this@MainActivity) }
                                .onFailure { Toast.makeText(this@MainActivity, it.message ?: "OAuth is not configured.", Toast.LENGTH_LONG).show() }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AppNavigation(navController: NavHostController, onConnectOAuth: () -> Unit) {
    val container = (androidx.compose.ui.platform.LocalContext.current.applicationContext as VibeSyncApplication).container
    NavHost(navController = navController, startDestination = "studio") {
        composable("studio") {
            val studioViewModel: PromptStudioViewModel = viewModel(factory = container.promptStudioFactory)
            PromptStudioScreen(studioViewModel, onOpenSettings = { navController.navigate("settings") })
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(factory = container.settingsFactory)
            SettingsScreen(settingsViewModel, onBack = { navController.popBackStack() }, onConnectOAuth = onConnectOAuth)
        }
    }
}
