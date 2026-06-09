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
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun MainScreen(authViewModel: AuthViewModel = viewModel()) {
    val context = LocalContext.current
    val currentUser by authViewModel.user.collectAsState()
    val startRoute = if (currentUser == null) NavRoute.Login else NavRoute.Portfolio
    val backStack = rememberNavBackStack(startRoute as NavKey)
    
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
                val portfolioVm: PortfolioViewModel = viewModel(factory = PortfolioViewModelFactory(userId))
                PortfolioScreen(viewModel = portfolioVm) 
            }
            entry<NavRoute.Vocab> { VocabScreen() }
            entry<NavRoute.Skills> { SkillsScreen() }
            entry<NavRoute.Journal> { JournalScreen() }
            entry<NavRoute.Admin> { AdminScreen() }
            entry<NavRoute.Settings> { SettingsScreen() }
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
                            if (pageTitle.isNotBlank()) {
                                Text(
                                    " • $pageTitle",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    actions = {
                        OutlinedButton(
                            onClick = { 
                                backStack.clear()
                                backStack.add(NavRoute.Settings) 
                            },
                            modifier = Modifier.padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Settings")
                        }
                        Button(
                            onClick = { authViewModel.signOut(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB91)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Logout", color = Color.Black)
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
