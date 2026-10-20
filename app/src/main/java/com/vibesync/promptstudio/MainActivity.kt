package com.vibesync.promptstudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibesync.promptstudio.data.diagnostics.ConnectivityStatus
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.PlayExperienceManager
import com.vibesync.promptstudio.presentation.DiagnosticsScreen
import com.vibesync.promptstudio.presentation.PromptStudioScreen
import com.vibesync.promptstudio.presentation.PromptStudioViewModel
import com.vibesync.promptstudio.presentation.SettingsScreen
import com.vibesync.promptstudio.presentation.SettingsViewModel
import com.vibesync.promptstudio.presentation.theme.VibeSyncTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var playExperience: PlayExperienceManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VibeSyncApplication
        val container = app.container
        playExperience = PlayExperienceManager(this, container.diagnostics)

        setContent {
            val connectivity by container.connectivityMonitor.observe()
                .collectAsStateWithLifecycle(initialValue = ConnectivityStatus.CONNECTING)
            VibeSyncTheme {
                AppNavigation(
                    navController = rememberNavController(),
                    connectivityStatus = connectivity,
                    onConnectOAuth = {
                        lifecycleScope.launch {
                            runCatching { container.oauthManager.launch(this@MainActivity) }
                                .onFailure {
                                    container.diagnostics.record(
                                        DiagnosticSeverity.WARNING,
                                        "auth",
                                        "oauth",
                                        "launch_failed",
                                        "OAuth launch failed: " + (it.message ?: it.javaClass.simpleName)
                                    )
                                    Toast.makeText(this@MainActivity, it.message ?: "OAuth is not configured.", Toast.LENGTH_LONG).show()
                                }
                        }
                    },
                    onManageAdsPrivacy = { container.adMobManager.showPrivacyOptions(this@MainActivity) },
                    onSuccessfulPromptRun = { provider, studioVisible ->
                        container.diagnostics.recordSuccessfulPromptRun(provider)
                        val interstitialStarted = container.adMobManager.onSuccessfulPromptRun(this@MainActivity, studioVisible)
                        playExperience?.onSuccessfulPromptRun(allowReview = !interstitialStarted && studioVisible)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playExperience?.onResume()
        (application as VibeSyncApplication).container.adMobManager.requestConsent(this)
    }

    override fun onStop() {
        playExperience?.onStop()
        super.onStop()
    }

    @Deprecated("Required for the Play In-App Updates result callback.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        playExperience?.onActivityResult(requestCode, resultCode)
    }
}

@androidx.compose.runtime.Composable
private fun AppNavigation(
    navController: NavHostController,
    connectivityStatus: ConnectivityStatus,
    onConnectOAuth: () -> Unit,
    onManageAdsPrivacy: () -> Unit,
    onSuccessfulPromptRun: (String, Boolean) -> Unit
) {
    val container = (androidx.compose.ui.platform.LocalContext.current.applicationContext as VibeSyncApplication).container
    NavHost(navController = navController, startDestination = "studio") {
        composable("studio") {
            val studioViewModel: PromptStudioViewModel = viewModel(factory = container.promptStudioFactory)
            PromptStudioScreen(
                viewModel = studioViewModel,
                onOpenSettings = { navController.navigate("settings") },
                onOpenDiagnostics = { navController.navigate("diagnostics") },
                connectivityStatus = connectivityStatus,
                adMobManager = container.adMobManager,
                onSuccessfulPromptRun = { provider ->
                    onSuccessfulPromptRun(provider, navController.currentDestination?.route == "studio")
                }
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(factory = container.settingsFactory)
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() },
                onConnectOAuth = onConnectOAuth,
                onOpenDiagnostics = { navController.navigate("diagnostics") },
                adMobManager = container.adMobManager,
                onManageAdsPrivacy = onManageAdsPrivacy
            )
        }
        composable("diagnostics") {
            DiagnosticsScreen(store = container.diagnostics, onBack = { navController.popBackStack() })
        }
    }
}
