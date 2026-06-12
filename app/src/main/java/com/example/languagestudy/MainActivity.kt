package com.example.languagestudy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.languagestudy.navigation.NavRoute
import com.example.languagestudy.navigation.icon
import com.example.languagestudy.navigation.label
import com.example.languagestudy.ui.auth.AuthViewModel
import com.example.languagestudy.ui.auth.LoginScreen
import com.example.languagestudy.ui.screens.*
import com.example.languagestudy.ui.theme.LanguageStudyTheme
import com.example.languagestudy.ui.viewmodel.PortfolioViewModel
import com.example.languagestudy.ui.viewmodel.PortfolioViewModelFactory
import com.example.languagestudy.ui.viewmodel.SearchViewModel
import com.example.languagestudy.ui.viewmodel.SettingsViewModel
import com.example.languagestudy.ui.viewmodel.SettingsViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the screen on during testing
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            LanguageStudyTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    authViewModel: AuthViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by authViewModel.user.collectAsState()
    val isAdmin by authViewModel.isAdmin.collectAsState()
    val scope = rememberCoroutineScope()
    
    val app = context.applicationContext as LanguageStudyApplication
    val settingsVm: SettingsViewModel = viewModel(
        key = "settings_${currentUser?.uid}",
        factory = SettingsViewModelFactory(app.settingsRepository, currentUser?.uid ?: "")
    )
    val userSettings by settingsVm.userSettings.collectAsState()

    val startRoute = if (currentUser == null) NavRoute.Login else NavRoute.Portfolio
    val backStack = rememberNavBackStack(startRoute as NavKey)
    
    var showMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val useNavRail = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val currentRoute = backStack.lastOrNull() as? NavRoute
    val pageTitle = currentRoute?.label ?: ""

    val provider = remember(currentUser) {
        entryProvider<NavKey> {
            entry<NavRoute.Login> { 
                LoginScreen(onLoginSuccess = {
                    backStack.clear()
                    backStack.add(NavRoute.Portfolio)
                }) 
            }
            entry<NavRoute.Portfolio> { 
                val userId = currentUser?.uid ?: ""
                val app = context.applicationContext as LanguageStudyApplication
                val portfolioVm: PortfolioViewModel = viewModel(
                    key = "portfolio_$userId",
                    factory = PortfolioViewModelFactory(userId, app.settingsRepository)
                )
                PortfolioScreen(viewModel = portfolioVm, searchViewModel = searchViewModel) 
            }
            entry<NavRoute.Vocab> { VocabScreen(currentUser?.uid ?: "", searchViewModel = searchViewModel) }
            entry<NavRoute.Skills> { SkillsScreen(currentUser?.uid ?: "", searchViewModel = searchViewModel) }
            entry<NavRoute.Journal> { JournalScreen(currentUser?.uid ?: "", searchViewModel = searchViewModel) }
            entry<NavRoute.Admin> { AdminScreen() }
            entry<NavRoute.Settings> { 
                val userId = currentUser?.uid ?: ""
                val app = context.applicationContext as LanguageStudyApplication
                val settingsVm: SettingsViewModel = viewModel(
                    key = "settings_$userId",
                    factory = SettingsViewModelFactory(app.settingsRepository, userId)
                )
                SettingsScreen(authViewModel = authViewModel, settingsViewModel = settingsVm) 
            }
        }
    }

    if (currentUser == null && backStack.lastOrNull() !is NavRoute.Login) {
        LaunchedEffect(Unit) {
            backStack.clear()
            backStack.add(NavRoute.Login)
        }
    }

    Scaffold(
        topBar = {
            if (currentUser != null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Language Study",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        val showSwitcher = userSettings.learnedLanguages.size > 1 && 
                                         currentRoute != NavRoute.Skills && 
                                         currentRoute != NavRoute.Journal
                        
                        if (showSwitcher) {
                            Box {
                                TextButton(onClick = { showLangMenu = true }) {
                                    Text(
                                        userSettings.languageLearning.ifBlank { "Select Lang" },
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
                                                settingsVm.setCurrentLanguage(lang)
                                                showLangMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Admin") },
                                        onClick = {
                                            showMenu = false
                                            backStack.clear()
                                            backStack.add(NavRoute.Admin)
                                        },
                                        leadingIcon = {
                                            Icon(androidx.compose.material.icons.Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showMenu = false
                                        backStack.clear()
                                        backStack.add(NavRoute.Settings)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Logout") },
                                    onClick = {
                                        showMenu = false
                                        authViewModel.signOut(context)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!useNavRail && currentUser != null && !WindowInsets.isImeVisible) {
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
        Row(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (useNavRail && currentUser != null) {
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
                modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.background),
                entryProvider = provider
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun MainScreenWidePreview() {
    LanguageStudyTheme {
        MainScreen()
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun MainScreenCompactPreview() {
    LanguageStudyTheme {
        MainScreen()
    }
}
