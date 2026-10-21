package com.vibesync.promptstudio.presentation.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vibesync.promptstudio.VibeSyncApplication
import com.vibesync.promptstudio.data.diagnostics.ConnectivityStatus
import com.vibesync.promptstudio.data.diagnostics.DiagnosticsStore
import com.vibesync.promptstudio.presentation.DeveloperResourcesScreen
import com.vibesync.promptstudio.presentation.DeveloperToolboxScreen
import com.vibesync.promptstudio.presentation.DiagnosticsScreen
import com.vibesync.promptstudio.presentation.PromptStudioScreen
import com.vibesync.promptstudio.presentation.PromptStudioViewModel
import com.vibesync.promptstudio.presentation.SettingsScreen
import com.vibesync.promptstudio.presentation.SettingsViewModel
import com.vibesync.promptstudio.presentation.auth.OAuthClientConfiguration
import com.vibesync.promptstudio.presentation.browser.AiWebStudioScreen
import com.vibesync.promptstudio.presentation.browser.BrowserPromptBridge
import com.vibesync.promptstudio.presentation.devoptions.DevOptionsScreen
import com.vibesync.promptstudio.presentation.home.HomeDashboardScreen
import com.vibesync.promptstudio.presentation.network.NetworkDnsDiagnosticsScreen
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavGraph(
    navController: NavHostController,
    connectivityStatus: ConnectivityStatus,
    isOnline: Boolean,
    onConnectOAuth: (OAuthClientConfiguration) -> Unit,
    onManageAdsPrivacy: () -> Unit,
    onSuccessfulPromptRun: (String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val container = (context.applicationContext as VibeSyncApplication).container

    NavHost(
        navController = navController,
        startDestination = "home_dashboard",
        enterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { it / 12 } },
        exitTransition = { fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { -it / 18 } },
        popEnterTransition = { fadeIn(tween(180)) + slideInHorizontally(tween(220)) { -it / 18 } },
        popExitTransition = { fadeOut(tween(140)) + slideOutHorizontally(tween(180)) { it / 12 } }
    ) {
        composable("home_dashboard") {
            HomeDashboardScreen(
                onOpenWebStudio = { navController.navigateTool("webstudio") },
                onOpenDeveloperOptions = { navController.navigateTool("devoptions") },
                onOpenPromptBank = { navController.navigateTool("developerhub?section=prompts") },
                onOpenDeveloperResources = { navController.navigateTool("developerhub?section=services") },
                onOpenToolbox = { navController.navigateTool("toolbox") },
                onOpenApiStudio = { navController.navigateTool("studio") },
                onOpenKeyVault = { navController.navigateTool("settings") },
                onOpenDiagnostics = { navController.navigateTool("diagnostics") },
                onOpenCommands = { navController.navigateTool("developerhub?section=commands") },
                onOpenNetworkDiagnostics = { navController.navigateTool("network_diagnostics") }
            )
        }
        composable("webstudio") {
            AiWebStudioScreen(
                onOpenDeveloperApi = { navController.navigateTool("studio") },
                onOpenDeveloperResources = { navController.navigateTool("developerhub?section=services") },
                onBack = { navController.returnHome() }
            )
        }
        composable("studio") {
            val studioViewModel: PromptStudioViewModel = viewModel(factory = container.promptStudioFactory)
            PromptStudioScreen(
                viewModel = studioViewModel,
                onOpenSettings = { navController.navigateTool("settings") },
                onOpenDiagnostics = { navController.navigateTool("diagnostics") },
                onOpenToolbox = { navController.navigateTool("toolbox") },
                onOpenDeveloperResources = { navController.navigateTool("developerhub?section=services") },
                onOpenWebStudio = { navController.navigateTool("webstudio") },
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
                onBack = { navController.returnHome() },
                onConnectOAuth = onConnectOAuth,
                onOpenDiagnostics = { navController.navigateTool("diagnostics") },
                adMobManager = container.adMobManager,
                onManageAdsPrivacy = onManageAdsPrivacy
            )
        }
        composable("diagnostics") {
            DiagnosticsScreen(store = container.diagnostics, onBack = { navController.returnHome() })
        }
        composable("toolbox") {
            DeveloperToolboxScreen(onBack = { navController.returnHome() })
        }
        composable(
            route = "developerhub?section={section}",
            arguments = listOf(navArgument("section") {
                type = NavType.StringType
                defaultValue = "prompts"
            })
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section") ?: "prompts"
            DeveloperResourcesScreen(
                onBack = { navController.returnHome() },
                onOpenWebStudio = { navController.navigateTool("webstudio") },
                onSendPromptToBrowser = { prompt ->
                    BrowserPromptBridge.enqueue(prompt)
                    navController.navigateTool("webstudio")
                },
                onOpenToolbox = { navController.navigateTool("toolbox") },
                onOpenApiStudio = { navController.navigateTool("studio") },
                onOpenDiagnostics = { navController.navigateTool("diagnostics") },
                onOpenSettings = { navController.navigateTool("settings") },
                initialSection = section
            )
        }
        composable("devoptions") {
            DevOptionsScreen(onBack = { navController.returnHome() })
        }
        composable("network_diagnostics") {
            NetworkDnsDiagnosticsScreen(onBack = { navController.returnHome() })
        }
    }
}

private fun NavHostController.navigateTool(route: String) {
    navigate(route) {
        popUpTo("home_dashboard") { inclusive = false }
        launchSingleTop = true
    }
}

private fun NavHostController.returnHome() {
    if (!popBackStack("home_dashboard", inclusive = false)) {
        navigate("home_dashboard") { launchSingleTop = true }
    }
}
