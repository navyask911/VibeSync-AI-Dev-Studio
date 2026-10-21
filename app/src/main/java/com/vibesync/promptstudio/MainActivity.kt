package com.vibesync.promptstudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibesync.promptstudio.data.diagnostics.ConnectivityStatus
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.PlayExperienceManager
import com.vibesync.promptstudio.data.diagnostics.InAppUpdateManager
import com.vibesync.promptstudio.presentation.DiagnosticsScreen
import com.vibesync.promptstudio.presentation.browser.AiWebStudioScreen
import com.vibesync.promptstudio.presentation.auth.OAuthClientConfiguration
import com.vibesync.promptstudio.presentation.DeveloperToolboxScreen
import com.vibesync.promptstudio.presentation.DeveloperResourcesScreen
import com.vibesync.promptstudio.presentation.browser.BrowserPromptBridge
import com.vibesync.promptstudio.presentation.PromptStudioScreen
import com.vibesync.promptstudio.presentation.PromptStudioViewModel
import com.vibesync.promptstudio.presentation.SettingsScreen
import com.vibesync.promptstudio.presentation.SettingsViewModel
import com.vibesync.promptstudio.presentation.theme.VibeSyncTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var playExperience: PlayExperienceManager? = null
    private var inAppUpdateManager: InAppUpdateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as VibeSyncApplication
        val container = app.container
        playExperience = PlayExperienceManager(this, container.diagnostics)
        inAppUpdateManager = InAppUpdateManager(this, container.diagnostics)

        setContent {
            val connectivity by container.connectivityMonitor.observe()
                .collectAsStateWithLifecycle(initialValue = ConnectivityStatus.CONNECTING)
            val isOnline by container.networkMonitor.isOnline.collectAsStateWithLifecycle()
            VibeSyncTheme {
                AppNavigation(
                    navController = rememberNavController(),
                    connectivityStatus = connectivity,
                    isOnline = isOnline,
                    onConnectOAuth = { configuration ->
                        lifecycleScope.launch {
                            runCatching { container.oauthManager.launch(this@MainActivity, configuration) }
                                .onFailure {
                                    container.diagnostics.record(
                                        DiagnosticSeverity.WARNING,
                                        "auth",
                                        "oauth",
                                        "launch_failed",
                                        "OAuth launch failed: " + (it.message ?: it.javaClass.simpleName)
                                    )
                                    Toast.makeText(this@MainActivity, it.message ?: getString(com.vibesync.promptstudio.R.string.oauth_not_configured), Toast.LENGTH_LONG).show()
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
        inAppUpdateManager?.onResume()
        (application as VibeSyncApplication).container.adMobManager.requestConsent(this)
    }

    override fun onStop() {
        inAppUpdateManager?.onStop()
        super.onStop()
    }

    @Deprecated("Required for the Play In-App Updates result callback.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        inAppUpdateManager?.onActivityResult(requestCode, resultCode)
    }
}

@androidx.compose.runtime.Composable
private fun AppNavigation(
    navController: NavHostController,
    connectivityStatus: ConnectivityStatus,
    isOnline: Boolean,
    onConnectOAuth: (OAuthClientConfiguration) -> Unit,
    onManageAdsPrivacy: () -> Unit,
    onSuccessfulPromptRun: (String, Boolean) -> Unit
) {
    val container = (androidx.compose.ui.platform.LocalContext.current.applicationContext as VibeSyncApplication).container
    val context = androidx.compose.ui.platform.LocalContext.current
    NavHost(navController = navController, startDestination = "webstudio") {
        composable("webstudio") {
            AiWebStudioScreen(
                onOpenDeveloperApi = { navController.navigate("studio") },
                onBack = {
                    if (!navController.popBackStack()) (context as? android.app.Activity)?.finish()
                }
            )
        }
        composable("studio") {
            val studioViewModel: PromptStudioViewModel = viewModel(factory = container.promptStudioFactory)
            PromptStudioScreen(
                viewModel = studioViewModel,
                onOpenSettings = { navController.navigate("settings") },
                onOpenDiagnostics = { navController.navigate("diagnostics") },
                onOpenToolbox = { navController.navigate("toolbox") },
                onOpenDeveloperResources = { navController.navigate("developerhub") },
                onOpenWebStudio = { navController.navigate("webstudio") },
                connectivityStatus = connectivityStatus,
                isOnline = isOnline,
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
        composable("toolbox") {
            DeveloperToolboxScreen(onBack = { navController.popBackStack() })
        }
        composable("developerhub") {
            DeveloperResourcesScreen(
                onBack = { navController.popBackStack() },
                onOpenWebStudio = { navController.navigate("webstudio") { launchSingleTop = true } },
                onSendPromptToBrowser = { prompt ->
                    BrowserPromptBridge.enqueue(prompt)
                    if (!navController.popBackStack("webstudio", false)) navController.navigate("webstudio") { launchSingleTop = true }
                },
                onOpenToolbox = { navController.navigate("toolbox") },
                onOpenApiStudio = { navController.navigate("studio") },
                onOpenDiagnostics = { navController.navigate("diagnostics") },
                onOpenSettings = { navController.navigate("settings") }
            )
        }
    }
}
