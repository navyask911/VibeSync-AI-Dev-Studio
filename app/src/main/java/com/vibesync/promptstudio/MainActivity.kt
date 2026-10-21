package com.vibesync.promptstudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibesync.promptstudio.presentation.AppLanguage
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


class MainActivity : AppCompatActivity() {
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

private data class MainNavigationItem(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

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
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route ?: "webstudio"
    var showLanguageSheet by remember { mutableStateOf(false) }
    val primaryRoutes = setOf("webstudio", "developerhub", "studio", "toolbox", "settings")
    val navigationItems = listOf(
        MainNavigationItem("webstudio", R.string.nav_web_studio, Icons.Default.Web),
        MainNavigationItem("developerhub", R.string.nav_prompt_bank, Icons.Default.Code),
        MainNavigationItem("studio", R.string.nav_api_studio, Icons.Default.Terminal),
        MainNavigationItem("toolbox", R.string.nav_developer_tools, Icons.Default.Build),
        MainNavigationItem("settings", R.string.nav_settings, Icons.Default.Settings)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    if (currentRoute == "diagnostics") {
                        IconButton(
                            onClick = {
                                if (!navController.popBackStack()) {
                                    navController.navigate("webstudio") { launchSingleTop = true }
                                }
                            },
                            modifier = Modifier.testTag("global_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.nav_back)
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLanguageSheet = true },
                        modifier = Modifier.testTag("global_language_selector")
                    ) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = stringResource(R.string.language_selector_open)
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (currentRoute in primaryRoutes) {
                NavigationBar {
                    navigationItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = stringResource(item.labelRes)
                                )
                            },
                            label = null,
                            alwaysShowLabel = false,
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.testTag("nav_${item.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "webstudio",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            composable("webstudio") {
                AiWebStudioScreen(
                    onOpenDeveloperApi = { navController.navigate("studio") },
                    onOpenDeveloperResources = { navController.navigate("developerhub") { launchSingleTop = true } },
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
                    },
                    showTopBar = false
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
                    onManageAdsPrivacy = onManageAdsPrivacy,
                    showTopBar = false
                )
            }
            composable("diagnostics") {
                DiagnosticsScreen(
                    store = container.diagnostics,
                    onBack = { navController.popBackStack() },
                    showTopBar = false
                )
            }
            composable("toolbox") {
                DeveloperToolboxScreen(onBack = { navController.popBackStack() }, showTopBar = false)
            }
            composable("developerhub") {
                DeveloperResourcesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWebStudio = { navController.navigate("webstudio") { launchSingleTop = true } },
                    onSendPromptToBrowser = { prompt ->
                        BrowserPromptBridge.enqueue(prompt)
                        if (!navController.popBackStack("webstudio", false)) {
                            navController.navigate("webstudio") { launchSingleTop = true }
                        }
                    },
                    onOpenToolbox = { navController.navigate("toolbox") },
                    onOpenApiStudio = { navController.navigate("studio") },
                    onOpenDiagnostics = { navController.navigate("diagnostics") },
                    onOpenSettings = { navController.navigate("settings") },
                    showTopBar = false
                )
            }
        }
    }

    if (showLanguageSheet) {
        val currentLanguageTag = AppCompatDelegate.getApplicationLocales().get(0)?.toLanguageTag()
            ?: java.util.Locale.getDefault().toLanguageTag()
        val selectedLanguage = AppLanguage.fromLanguageTag(currentLanguageTag)
        ModalBottomSheet(onDismissRequest = { showLanguageSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.language_selector_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showLanguageSheet = false
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(language.languageTag)
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("language_${language.languageTag}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == selectedLanguage,
                            onClick = {
                                showLanguageSheet = false
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(language.languageTag)
                                )
                            }
                        )
                        Text(
                            text = stringResource(language.labelRes),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

