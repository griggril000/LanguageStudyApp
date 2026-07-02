package io.github.langstudy

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowSizeClass
import io.github.langstudy.navigation.NavRoute
import io.github.langstudy.navigation.icon
import io.github.langstudy.navigation.label
import io.github.langstudy.ui.auth.AuthViewModel
import io.github.langstudy.ui.auth.AuthViewModelFactory
import io.github.langstudy.ui.auth.LoginScreen
import io.github.langstudy.ui.screens.AdminScreen
import io.github.langstudy.ui.screens.JournalScreen
import io.github.langstudy.ui.screens.PortfolioScreen
import io.github.langstudy.ui.screens.SettingsScreen
import io.github.langstudy.ui.screens.SkillsScreen
import io.github.langstudy.ui.screens.VocabScreen
import io.github.langstudy.ui.screens.WelcomeWalkthrough
import io.github.langstudy.ui.theme.LanguageStudyTheme
import io.github.langstudy.ui.viewmodel.AdminViewModel
import io.github.langstudy.ui.viewmodel.AdminViewModelFactory
import io.github.langstudy.ui.viewmodel.PortfolioViewModel
import io.github.langstudy.ui.viewmodel.PortfolioViewModelFactory
import io.github.langstudy.ui.viewmodel.SearchViewModel
import io.github.langstudy.ui.viewmodel.SettingsViewModel
import io.github.langstudy.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow

class MainActivity : ComponentActivity() {
    private val intentFlow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            MainScreen(intentFlow = intentFlow)
        }
        val currentIntent = intent
        if (currentIntent != null) {
            intentFlow.tryEmit(currentIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentFlow.tryEmit(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    intentFlow: Flow<Intent> = emptyFlow()
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication

    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(app.adminRepository)
    )
    val searchViewModel: SearchViewModel = viewModel()

    val currentUser by authViewModel.user.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val isMentorMode by authViewModel.isMentorMode.collectAsState()
    val effectiveUserId by authViewModel.effectiveUserId.collectAsState()

    val settingsVm: SettingsViewModel = viewModel(
        key = "settings_$effectiveUserId",
        factory = SettingsViewModelFactory(
            app.settingsRepository,
            app.mentorRepository,
            effectiveUserId,
            app.vocabRepository,
            app.skillRepository,
            app.portfolioRepository,
            app.journalRepository
        )
    )
    val userSettings by settingsVm.userSettings.collectAsState()

    val darkTheme = when (userSettings.theme) {
        "light" -> false
        "dark" -> true
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    LanguageStudyTheme(
        darkTheme = darkTheme,
        dynamicColor = userSettings.theme == "dynamic"
    ) {
        val activity = LocalActivity.current as? ComponentActivity
        LaunchedEffect(darkTheme) {
            activity?.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { darkTheme },
                navigationBarStyle = SystemBarStyle.auto(
                    android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF),
                    android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                ) { darkTheme }
            )
        }

        val startRoute = remember(currentUser) {
            if (currentUser == null) NavRoute.Login else NavRoute.fromString(userSettings.homepageTab)
        }
        val backStack = rememberNavBackStack(startRoute)

        LaunchedEffect(userSettings.homepageTab) {
            if (currentUser != null && backStack.size == 1 && backStack.lastOrNull() == NavRoute.Vocab) {
                val preferred = NavRoute.fromString(userSettings.homepageTab)
                if (preferred != NavRoute.Vocab) {
                    backStack.clear()
                    backStack.add(preferred)
                }
            }
        }

        LaunchedEffect(currentUser) {
            if (currentUser != null) {
//                if (currentUser?.email == "test@example.com") {
//                    val seeder = SampleDataSeeder(
//                        app.vocabRepository,
//                        app.skillRepository,
//                        app.settingsRepository,
//                        app.portfolioRepository,
//                        app.journalRepository
//                    )
//                    launch { seeder.seed(currentUser!!.uid) }
//                }
                intentFlow.collect { intent ->
                    if (intent.action == Intent.ACTION_VIEW) {
                        val data = intent.data
                        if (data?.host == "language-study.github.io" && (data.path == "/index.html" || data.path == "/")) {
                            val code = data.getQueryParameter("mentor")
                            if (code != null && code.length == 5) {
                                val ownerUid = settingsVm.validateCode(code)
                                if (ownerUid != null && ownerUid != currentUser?.uid) {
                                    authViewModel.enterMentorMode(ownerUid, code)
                                    backStack.clear()
                                    backStack.add(NavRoute.Portfolio)
                                }
                            }
                        }
                    }
                }
            }
        }

        var showMenu by remember { mutableStateOf(false) }
        var showLangMenu by remember { mutableStateOf(false) }
        var showResources by remember { mutableStateOf(false) }

        val adaptiveInfo = currentWindowAdaptiveInfoV2()
        val useNavRail =
            adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

        val provider = remember(currentUser, effectiveUserId) {
            entryProvider<NavKey> {
                entry<NavRoute.Login> {
                    LoginScreen(onLoginSuccess = {
                        backStack.clear()
                        backStack.add(NavRoute.fromString(userSettings.homepageTab))
                    })
                }
                entry<NavRoute.Vocab> {
                    VocabScreen(
                        userId = effectiveUserId,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = userSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Skills> {
                    SkillsScreen(
                        userId = effectiveUserId,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = userSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Portfolio> {
                    val userId = effectiveUserId
                    val portfolioVm: PortfolioViewModel = viewModel(
                        key = "portfolio_$userId",
                        factory = PortfolioViewModelFactory(userId, app.settingsRepository)
                    )
                    PortfolioScreen(
                        viewModel = portfolioVm,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = userSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Journal> {
                    JournalScreen(
                        userId = effectiveUserId,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = userSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Admin> {
                    val adminVm: AdminViewModel = viewModel(
                        factory = AdminViewModelFactory(app.adminRepository, app.settingsRepository)
                    )
                    adminVm.initUserId(effectiveUserId)
                    AdminScreen(
                        viewModel = adminVm,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
                    )
                }
                entry<NavRoute.Settings> {
                    SettingsScreen(
                        authViewModel = authViewModel,
                        settingsViewModel = settingsVm,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
                    )
                }
            }
        }

        if (currentUser == null && backStack.lastOrNull() !is NavRoute.Login) {
            LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(NavRoute.Login)
            }
        }

        if (showResources) {
            LanguageResourcesDialog(
                viewModel = settingsVm,
                onDismiss = { showResources = false }
            )
        }

        var showWalkthrough by remember { mutableStateOf(false) }
        LaunchedEffect(userSettings.firstLogin) {
            if (userSettings.firstLogin) {
                showWalkthrough = true
            }
        }

        if (currentUser != null && showWalkthrough) {
            WelcomeWalkthrough(
                viewModel = settingsVm,
                email = currentUser?.email,
                onDismiss = {
                    settingsVm.setFirstLogin(false)
                    showWalkthrough = false
                },
                onFinish = {
                    settingsVm.setFirstLogin(false)
                    showWalkthrough = false
                }
            )
        }

        Scaffold(
            topBar = {
                if (currentUser != null &&
                    backStack.lastOrNull() != NavRoute.Settings &&
                    backStack.lastOrNull() != NavRoute.Admin
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    if (isMentorMode) "Mentor View" else "Language Study",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isMentorMode) {
                                    val mentorCode by authViewModel.mentorCode.collectAsState()
                                    Text(
                                        "Code: $mentorCode",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isMentorMode) {
                                TextButton(onClick = {
                                    authViewModel.exitMentorMode()
                                    searchViewModel.setSelectedLanguage(null)
                                }) {
                                    Text("Exit")
                                }
                            }

                            val showSwitcher = userSettings.learnedLanguages.isNotEmpty()

                            if (showSwitcher) {
                                val languageOverride by searchViewModel.selectedLanguage.collectAsState()
                                val displayLanguage =
                                    languageOverride ?: userSettings.languageLearning
                                Box {
                                    TextButton(onClick = { showLangMenu = true }) {
                                        Text(
                                            displayLanguage.ifBlank { "Select Lang" },
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showLangMenu,
                                        onDismissRequest = { showLangMenu = false }
                                    ) {
                                        userSettings.learnedLanguages.forEach { lang ->
                                            DropdownMenuItem(
                                                text = { Text(lang) },
                                                onClick = {
                                                    if (isMentorMode) {
                                                        searchViewModel.setSelectedLanguage(lang)
                                                    } else {
                                                        settingsVm.setCurrentLanguage(lang)
                                                        // Also clear override when setting persistent language
                                                        searchViewModel.setSelectedLanguage(null)
                                                    }
                                                    showLangMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        Icons.Rounded.MoreVert,
                                        contentDescription = "More options"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Study Resources") },
                                        onClick = {
                                            showMenu = false
                                            showResources = true
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.AutoMirrored.Rounded.MenuBook,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                    if (isAdmin) {
                                        DropdownMenuItem(
                                            text = { Text("Admin") },
                                            onClick = {
                                                showMenu = false
                                                if (backStack.lastOrNull() != NavRoute.Admin) {
                                                    backStack.add(NavRoute.Admin)
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Rounded.AdminPanelSettings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Settings") },
                                        onClick = {
                                            showMenu = false
                                            if (backStack.lastOrNull() != NavRoute.Settings) {
                                                backStack.add(NavRoute.Settings)
                                            }
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.Settings,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                    )
                }
            },
            bottomBar = {
                val currentRoute = backStack.lastOrNull() as? NavRoute
                val isFullScreen = currentRoute == NavRoute.Settings || currentRoute == NavRoute.Admin
                if (!useNavRail && currentUser != null && !WindowInsets.isImeVisible && !isFullScreen) {
                    NavigationBar {
                        NavRoute.mainRoutes.forEach { route ->
                            NavigationBarItem(
                                selected = backStack.lastOrNull() == route,
                                onClick = {
                                    if (backStack.lastOrNull() != route) {
                                        backStack.clear()
                                        backStack.add(route)
                                    }
                                },
                                icon = { Icon(route.icon, contentDescription = route.label) },
                                label = { Text(route.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            val layoutDirection = LocalLayoutDirection.current
            val currentRoute = backStack.lastOrNull() as? NavRoute
            val isFullScreen = currentRoute == NavRoute.Settings || currentRoute == NavRoute.Admin

            Row(
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = innerPadding.calculateStartPadding(layoutDirection),
                        top = if (isFullScreen) 0.dp else innerPadding.calculateTopPadding(),
                        end = innerPadding.calculateEndPadding(layoutDirection),
                        bottom = if (isFullScreen) 0.dp else innerPadding.calculateBottomPadding()
                    )
            ) {
                if (useNavRail && currentUser != null && !isFullScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        NavRoute.mainRoutes.forEach { route ->
                            NavigationRailItem(
                                selected = backStack.lastOrNull() == route,
                                onClick = {
                                    if (backStack.lastOrNull() != route) {
                                        backStack.clear()
                                        backStack.add(route)
                                    }
                                },
                                icon = { Icon(route.icon, contentDescription = route.label) },
                                label = { Text(route.label) }
                            )
                        }
                    }
                }
                NavDisplay(
                    backStack = backStack,
                    onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                        .let { 
                            if (!isFullScreen) it.consumeWindowInsets(innerPadding) else it
                        },
                    entryProvider = provider
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageResourcesDialog(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    val userSettings by viewModel.userSettings.collectAsState()
    val resources by viewModel.resources.collectAsState()
    val resourceLanguage by viewModel.resourceLanguage.collectAsState()
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Study Resources")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Language Selector
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                resourceLanguage.ifBlank { "Select Language" },
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        userSettings.learnedLanguages.forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang) },
                                onClick = {
                                    viewModel.setResourceLanguage(lang)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (resources.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No resources found for $resourceLanguage",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        resources.forEach { resource ->
                            Card(
                                onClick = { uriHandler.openUri(resource.url) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Link,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        resource.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
