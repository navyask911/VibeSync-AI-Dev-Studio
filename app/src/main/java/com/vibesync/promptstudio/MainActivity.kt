package com.vibesync.promptstudio

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibesync.promptstudio.data.diagnostics.ConnectivityStatus
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.InAppUpdateManager
import com.vibesync.promptstudio.data.diagnostics.PlayExperienceManager
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
import com.vibesync.promptstudio.presentation.theme.VibeSyncTheme
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private var playExperience: PlayExperienceManager? = null
    private var inAppUpdateManager: InAppUpdateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VibeSyncApplication).container
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
                                    Toast.makeText(
                                        this@MainActivity,
                                        it.message ?: getString(R.string.oauth_not_configured),
                                        Toast.LENGTH_LONG
                                    ).show()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppNavigation(
    navController: NavHostController,
    connectivityStatus: ConnectivityStatus,
    isOnline: Boolean,
    onConnectOAuth: (OAuthClientConfiguration) -> Unit,
    onManageAdsPrivacy: () -> Unit,
    onSuccessfulPromptRun: (String, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as VibeSyncApplication).container
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route ?: "webstudio"
    val localeTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
    val selectedLanguage = normalizeAppLanguageTag(localeTags.ifBlank { Locale.getDefault().toLanguageTag() })
    var showLanguageSheet by remember { mutableStateOf(false) }
    val isPrimaryRoute = route in primaryNavigationRoutes

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets.statusBars,
                navigationIcon = {
                    if (!isPrimaryRoute) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLanguageSheet = true }) {
                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language_picker_content_description))
                    }
                }
            )
        },
        bottomBar = {
            val selectedRoute = when (route) {
                "settings", "diagnostics" -> "studio"
                "toolbox" -> "developerhub"
                else -> route
            }
            NavigationBar(
                windowInsets = WindowInsets.navigationBars,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                appNavigationItems.forEach { item ->
                    val selected = selectedRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (route != item.route) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Surface(
                                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                shape = RoundedCornerShape(percent = 50)
                            ) {
                                Icon(
                                    item.icon,
                                    contentDescription = stringResource(item.labelRes),
                                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp)
                                )
                            }
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "webstudio",
            modifier = Modifier.fillMaxSize().padding(innerPadding)
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
                DiagnosticsScreen(store = container.diagnostics, onBack = { navController.popBackStack() }, showTopBar = false)
            }
            composable("toolbox") {
                DeveloperToolboxScreen(onBack = { navController.popBackStack() }, showTopBar = false)
            }
            composable("promptbank") {
                DeveloperResourcesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWebStudio = { navController.navigate("webstudio") { launchSingleTop = true } },
                    onSendPromptToBrowser = { prompt ->
                        BrowserPromptBridge.enqueue(prompt)
                        navController.navigate("webstudio") { launchSingleTop = true }
                    },
                    onOpenToolbox = { navController.navigate("toolbox") },
                    onOpenApiStudio = { navController.navigate("studio") },
                    onOpenDiagnostics = { navController.navigate("diagnostics") },
                    onOpenSettings = { navController.navigate("settings") },
                    initialSection = "prompts"
                )
            }
            composable("developerhub") {
                DeveloperResourcesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWebStudio = { navController.navigate("webstudio") { launchSingleTop = true } },
                    onSendPromptToBrowser = { prompt ->
                        BrowserPromptBridge.enqueue(prompt)
                        navController.navigate("webstudio") { launchSingleTop = true }
                    },
                    onOpenToolbox = { navController.navigate("toolbox") },
                    onOpenApiStudio = { navController.navigate("studio") },
                    onOpenDiagnostics = { navController.navigate("diagnostics") },
                    onOpenSettings = { navController.navigate("settings") },
                    initialSection = "services"
                )
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(stringResource(R.string.language_picker_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.language_rtl_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                supportedAppLanguages.forEach { language ->
                    val selected = language.tag == selectedLanguage
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable {
                            showLanguageSheet = false
                            if (language.tag != selectedLanguage) {
                                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                stringResource(language.labelRes),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (selected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = stringResource(R.string.language_picker_content_description),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.padding(bottom = 10.dp))
            }
        }
    }
}

private val primaryNavigationRoutes = setOf("webstudio", "studio", "promptbank", "developerhub")

private data class AppNavigationItem(
    val route: String,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val appNavigationItems = listOf(
    AppNavigationItem("webstudio", R.string.nav_web_studio, Icons.Default.Web),
    AppNavigationItem("studio", R.string.nav_api_diagnostics, Icons.Default.Code),
    AppNavigationItem("promptbank", R.string.nav_prompt_bank, Icons.Default.MenuBook),
    AppNavigationItem("developerhub", R.string.nav_resources, Icons.Default.Folder)
)
