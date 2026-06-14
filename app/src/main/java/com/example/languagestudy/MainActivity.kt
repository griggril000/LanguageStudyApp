package com.example.languagestudy

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
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
    val isMentorMode by authViewModel.isMentorMode.collectAsState()
    val effectiveUserId by authViewModel.effectiveUserId.collectAsState()
    
    val app = context.applicationContext as LanguageStudyApplication
    val settingsVm: SettingsViewModel = viewModel(
        key = "settings_$effectiveUserId",
        factory = SettingsViewModelFactory(app.settingsRepository, app.mentorRepository, effectiveUserId)
    )
    val userSettings by settingsVm.userSettings.collectAsState()

    val startRoute = if (currentUser == null) NavRoute.Login else NavRoute.Portfolio
    val backStack = rememberNavBackStack(startRoute as NavKey)
    
    var showMenu by remember { mutableStateOf(false) }
    var showLangMenu by remember { mutableStateOf(false) }
    var showResources by remember { mutableStateOf(false) }

    val adaptiveInfo = currentWindowAdaptiveInfo()
    val useNavRail = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val currentRoute = backStack.lastOrNull() as? NavRoute

    val provider = remember(currentUser, effectiveUserId) {
        entryProvider<NavKey> {
            entry<NavRoute.Login> { 
                LoginScreen(onLoginSuccess = {
                    backStack.clear()
                    backStack.add(NavRoute.Portfolio)
                }) 
            }
            entry<NavRoute.Portfolio> { 
                val userId = effectiveUserId
                val portfolioVm: PortfolioViewModel = viewModel(
                    key = "portfolio_$userId",
                    factory = PortfolioViewModelFactory(userId, app.settingsRepository)
                )
                PortfolioScreen(viewModel = portfolioVm, searchViewModel = searchViewModel) 
            }
            entry<NavRoute.Vocab> { VocabScreen(effectiveUserId, searchViewModel = searchViewModel) }
            entry<NavRoute.Skills> { SkillsScreen(effectiveUserId, searchViewModel = searchViewModel) }
            entry<NavRoute.Journal> { JournalScreen(effectiveUserId, searchViewModel = searchViewModel) }
            entry<NavRoute.Admin> { AdminScreen() }
            entry<NavRoute.Settings> { 
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

    if (showResources) {
        LanguageResourcesDialog(
            viewModel = settingsVm,
            onDismiss = { showResources = false }
        )
    }

    Scaffold(
        topBar = {
            if (currentUser != null) {
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
                                Text("Code: $mentorCode", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    },
                    actions = {
                        if (isMentorMode) {
                            TextButton(onClick = { authViewModel.exitMentorMode() }) {
                                Text("Exit")
                            }
                        }
                        
                        val showSwitcher = userSettings.learnedLanguages.isNotEmpty() &&
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
                                DropdownMenuItem(
                                    text = { Text("Study Resources") },
                                    onClick = {
                                        showMenu = false
                                        showResources = true
                                    },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                                if (isAdmin) {
                                    DropdownMenuItem(
                                        text = { Text("Admin") },
                                        onClick = {
                                            showMenu = false
                                            backStack.clear()
                                            backStack.add(NavRoute.Admin)
                                        },
                                        leadingIcon = { Icon(Icons.Rounded.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Settings") },
                                    onClick = {
                                        showMenu = false
                                        backStack.clear()
                                        backStack.add(NavRoute.Settings)
                                    },
                                    leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
        Row(Modifier.fillMaxSize().padding(innerPadding)) {
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                            Text(resourceLanguage.ifBlank { "Select Language" }, fontWeight = FontWeight.Bold)
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
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("No resources found for $resourceLanguage", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        resources.forEach { resource ->
                            Card(
                                onClick = { uriHandler.openUri(resource.url) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(resource.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
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
