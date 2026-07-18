package io.github.langstudy

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import io.github.langstudy.ui.auth.VerifyEmailScreen
import io.github.langstudy.ui.screens.AdminScreen
import io.github.langstudy.ui.screens.FlashcardScreen
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
import io.github.langstudy.utils.LocalTtsManager
import io.github.langstudy.utils.TtsManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow

class MainActivity : ComponentActivity() {
    private val intentChannel = Channel<Intent>(capacity = Channel.CONFLATED)
    private lateinit var ttsManager: TtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ttsManager = TtsManager(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            androidx.compose.runtime.CompositionLocalProvider(LocalTtsManager provides ttsManager) {
                MainScreen(intentFlow = intentChannel.receiveAsFlow())
            }
        }
        val currentIntent = intent
        if (currentIntent != null) {
            intentChannel.trySend(currentIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intentChannel.trySend(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        ttsManager.shutdown()
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
    val sessionId by authViewModel.sessionId.collectAsState()
    val searchViewModel: SearchViewModel = viewModel(key = "search_$sessionId")

    val currentUser by authViewModel.user.collectAsState()
    val isEmailVerified by authViewModel.isEmailVerified.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val isMentorMode by authViewModel.isMentorMode.collectAsState()
    val effectiveUserId by authViewModel.effectiveUserId.collectAsState()

    // Always use the ACTUAL user's ID for app-wide settings like theme and homepage.
    // This prevents PERMISSION_DENIED errors when in Mentor Mode and keeps the mentor's
    // own theme/preferences active.
    val actualUserId = currentUser?.uid ?: ""
    val settingsVm: SettingsViewModel = viewModel(
        key = "settings_${actualUserId}_$sessionId",
        factory = SettingsViewModelFactory(
            app.settingsRepository,
            app.mentorRepository,
            actualUserId,
            app.vocabRepository,
            app.skillRepository,
            app.portfolioRepository,
            app.journalRepository
        )
    )

    // We also need a version of the settings VM for the effective user to show counts/data
    // in the UI, but we'll only use it where needed to avoid unnecessary listeners.
    val effectiveSettingsVm: SettingsViewModel = viewModel(
        key = "settings_data_${effectiveUserId}_$sessionId",
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
    val effectiveUserSettings by effectiveSettingsVm.userSettings.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser?.email == "test@example.com") {
            app.sampleDataSeeder.seed(currentUser?.uid ?: "")
        }
    }

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

        val startRoute = remember(currentUser, isEmailVerified) {
            if (currentUser == null) {
                NavRoute.Login
            } else if (!isEmailVerified) {
                NavRoute.VerifyEmail
            } else {
                NavRoute.fromString(userSettings.homepageTab)
            }
        }
        val backStack = rememberNavBackStack(startRoute)

        var showMenu by remember { mutableStateOf(false) }
        var showLangMenu by remember { mutableStateOf(false) }
        var showResources by remember { mutableStateOf(false) }

        LaunchedEffect(userSettings.homepageTab) {
            if (currentUser != null && backStack.size == 1 && backStack.lastOrNull() == NavRoute.Vocab) {
                val preferred = NavRoute.fromString(userSettings.homepageTab)
                if (preferred != NavRoute.Vocab) {
                    backStack.clear()
                    backStack.add(preferred)
                }
            }
        }

        LaunchedEffect(Unit) {
            intentFlow.collect { intent ->
                if (intent.action == Intent.ACTION_VIEW) {
                    val data = intent.data
                    Log.d("DeepLink", "Received URI: $data")
                    if (data?.host?.contains("language-study.github.io") == true || data?.scheme == "langstudy") {
                        val mode = data.getQueryParameter("mode")
                        val oobCode = data.getQueryParameter("oobCode")

                        if (mode == "verifyEmail" && oobCode != null) {
                            Log.d("DeepLink", "Verifying email with code: $oobCode")
                            authViewModel.verifyEmail(oobCode)
                        }

                        // Wait for user to be logged in before processing deep link
                        authViewModel.user.filterNotNull().first()

                        var mentorParam = data.getQueryParameter("mentor")
                        var tabParam = data.getQueryParameter("tab")

                        // Handle login redirect case where params are in returnTo
                        if (mentorParam == null && tabParam == null) {
                            data.getQueryParameter("returnTo")?.let { returnTo ->
                                try {
                                    val returnUri = returnTo.toUri()
                                    mentorParam = returnUri.getQueryParameter("mentor")
                                    tabParam = returnUri.getQueryParameter("tab")
                                } catch (e: Exception) {
                                    Log.e("DeepLink", "Error parsing returnTo URI", e)
                                }
                            }
                        }

                        // Sanitize mentor code (take first 5 chars to avoid duplication issues from some sources)
                        val code = mentorParam?.take(5)
                        val tab = tabParam
                        val action = data.getQueryParameter("action")

                        Log.d(
                            "DeepLink",
                            "Final Parsed - mentor: $code, tab: $tab, action: $action, mode: $mode"
                        )

                        if (action == "resources") {
                            showResources = true
                        }

                        if (code != null && code.length == 5) {
                            val mentorCodeState = authViewModel.mentorCode.value
                            val currentUid = authViewModel.user.value?.uid

                            if (code != mentorCodeState) {
                                Log.d("DeepLink", "New mentor code detected. Validating...")
                                val ownerUid = settingsVm.validateCode(code)
                                if (ownerUid != null && ownerUid != currentUid) {
                                    Log.d("DeepLink", "Entering mentor mode for $ownerUid")
                                    authViewModel.enterMentorMode(context, ownerUid, code)
                                    backStack.clear()
                                    val targetRoute =
                                        if (tab != null) {
                                            if (tab.lowercase() == "journal") {
                                                NavRoute.Journal(
                                                    openEntry = data.getBooleanQueryParameter(
                                                        "openEntry",
                                                        false
                                                    )
                                                )
                                            } else NavRoute.fromString(tab)
                                        } else NavRoute.Portfolio
                                    backStack.add(targetRoute)
                                } else {
                                    Log.w(
                                        "DeepLink",
                                        "Invalid code or trying to mentor self. ownerUid: $ownerUid"
                                    )
                                }
                            } else {
                                Log.d(
                                    "DeepLink",
                                    "Already in mentor mode with code $code. Checking tab: $tab"
                                )
                                tab?.let {
                                    val targetRoute = if (it.lowercase() == "journal") {
                                        NavRoute.Journal(
                                            openEntry = data.getBooleanQueryParameter(
                                                "openEntry",
                                                false
                                            )
                                        )
                                    } else NavRoute.fromString(it)

                                    if (backStack.lastOrNull() != targetRoute) {
                                        Log.d("DeepLink", "Navigating to tab: $targetRoute")
                                        backStack.clear()
                                        backStack.add(targetRoute)
                                    }
                                }
                            }
                        } else if (tab != null) {
                            Log.d("DeepLink", "No mentor code, navigating to tab: $tab")
                            val targetRoute = if (tab.lowercase() == "journal") {
                                NavRoute.Journal(
                                    openEntry = data.getBooleanQueryParameter(
                                        "openEntry",
                                        false
                                    )
                                )
                            } else NavRoute.fromString(tab)

                            if (backStack.lastOrNull() != targetRoute) {
                                backStack.clear()
                                backStack.add(targetRoute)
                            }
                        }
                    }
                }
            }
        }

        val adaptiveInfo = currentWindowAdaptiveInfoV2()
        val useNavRail =
            adaptiveInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

        val provider = remember(currentUser, effectiveUserId) {
            entryProvider<NavKey> {
                entry<NavRoute.Login> {
                    LoginScreen(onLoginSuccess = {
                        backStack.clear()
                        if (authViewModel.isEmailVerified.value) {
                            backStack.add(NavRoute.fromString(userSettings.homepageTab))
                        } else {
                            backStack.add(NavRoute.VerifyEmail)
                        }
                    })
                }
                entry<NavRoute.VerifyEmail> {
                    VerifyEmailScreen(
                        viewModel = authViewModel,
                        onBackToLogin = {
                            authViewModel.signOut(context)
                        }
                    )
                }
                entry<NavRoute.Vocab> {
                    VocabScreen(
                        userId = effectiveUserId,
                        onNavigate = { route ->
                            if (backStack.lastOrNull() != route) {
                                backStack.add(route)
                            }
                        },
                        sessionId = sessionId,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = effectiveUserSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Flashcards> { route ->
                    FlashcardScreen(
                        userId = effectiveUserId,
                        categoryFilter = route.category,
                        languageFilter = route.language,
                        onClose = {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            } else {
                                backStack.clear()
                                backStack.add(NavRoute.Vocab)
                            }
                        }
                    )
                }
                entry<NavRoute.Skills> {
                    SkillsScreen(
                        userId = effectiveUserId,
                        sessionId = sessionId,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = effectiveUserSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Portfolio> {
                    val userId = effectiveUserId
                    val portfolioVm: PortfolioViewModel = viewModel(
                        key = "portfolio_${userId}_$sessionId",
                        factory = PortfolioViewModelFactory(userId, app.settingsRepository)
                    )
                    PortfolioScreen(
                        viewModel = portfolioVm,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = effectiveUserSettings.mentorAccessLevel
                    )
                }
                entry<NavRoute.Journal> { route ->
                    JournalScreen(
                        userId = effectiveUserId,
                        sessionId = sessionId,
                        searchViewModel = searchViewModel,
                        isMentorMode = isMentorMode,
                        mentorAccessLevel = effectiveUserSettings.mentorAccessLevel,
                        openEntry = route.openEntry
                    )
                }
                entry<NavRoute.Admin> {
                    val adminVm: AdminViewModel = viewModel(
                        key = "admin_$sessionId",
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
                        settingsViewModel = effectiveSettingsVm,
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

        if (currentUser != null && !isEmailVerified && backStack.lastOrNull() !is NavRoute.VerifyEmail) {
            LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(NavRoute.VerifyEmail)
            }
        }

        if (currentUser != null && isEmailVerified && backStack.lastOrNull() is NavRoute.VerifyEmail) {
            LaunchedEffect(Unit) {
                backStack.clear()
                backStack.add(NavRoute.fromString(userSettings.homepageTab))
            }
        }

        if (showResources) {
            LanguageResourcesDialog(
                viewModel = settingsVm,
                onDismiss = { showResources = false }
            )
        }

        val showWalkthrough = userSettings.firstLogin && isEmailVerified

        if (currentUser != null && isEmailVerified && showWalkthrough) {
            WelcomeWalkthrough(
                viewModel = settingsVm,
                email = currentUser?.email,
                onDismiss = {
                    settingsVm.setFirstLogin(false)
                },
                onFinish = {
                    settingsVm.setFirstLogin(false)
                }
            )
        }

        Scaffold(
            topBar = {
                if (currentUser != null &&
                    backStack.lastOrNull() != NavRoute.Settings &&
                    backStack.lastOrNull() != NavRoute.Admin &&
                    backStack.lastOrNull() != NavRoute.VerifyEmail
                ) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(
                                    if (isMentorMode) stringResource(R.string.mentor_view) else stringResource(
                                        R.string.app_name
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isMentorMode) {
                                    val mentorCode by authViewModel.mentorCode.collectAsState()
                                    Text(
                                        stringResource(
                                            R.string.mentor_code_format,
                                            mentorCode ?: ""
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isMentorMode) {
                                TextButton(onClick = {
                                    authViewModel.exitMentorMode(context)
                                    searchViewModel.setSelectedLanguage(null)
                                }) {
                                    Text(stringResource(R.string.exit))
                                }
                            }

                            val effectiveUserSettings by effectiveSettingsVm.userSettings.collectAsState()
                            val showSwitcher = effectiveUserSettings.learnedLanguages.isNotEmpty()

                            if (showSwitcher) {
                                val languageOverride by searchViewModel.selectedLanguage.collectAsState()
                                val displayLanguage =
                                    languageOverride ?: effectiveUserSettings.languageLearning
                                Box {
                                    TextButton(onClick = { showLangMenu = true }) {
                                        Text(
                                            displayLanguage.ifBlank { stringResource(R.string.select_lang) },
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showLangMenu,
                                        onDismissRequest = { showLangMenu = false }
                                    ) {
                                        effectiveUserSettings.learnedLanguages.forEach { lang ->
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
                                        contentDescription = stringResource(R.string.more_options_cd)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.study_resources)) },
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
                                            text = { Text(stringResource(R.string.admin)) },
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
                                        text = { Text(stringResource(R.string.settings)) },
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
                val isFullScreen =
                    currentRoute == NavRoute.Settings || currentRoute == NavRoute.Admin || currentRoute == NavRoute.VerifyEmail
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
            val isFullScreen = currentRoute == NavRoute.Settings || currentRoute == NavRoute.Admin || currentRoute == NavRoute.VerifyEmail

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
                Text(stringResource(R.string.study_resources))
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
                                resourceLanguage.ifBlank { stringResource(R.string.select_lang) },
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
                            stringResource(R.string.no_resources_found_format, resourceLanguage),
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        }
    )
}
