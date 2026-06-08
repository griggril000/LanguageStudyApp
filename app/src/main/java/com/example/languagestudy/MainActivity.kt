package com.example.languagestudy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.entryProvider
import androidx.window.core.layout.WindowWidthSizeClass
import com.example.languagestudy.navigation.NavRoute
import com.example.languagestudy.navigation.icon
import com.example.languagestudy.navigation.label
import com.example.languagestudy.ui.screens.*
import com.example.languagestudy.ui.theme.LanguageStudyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LanguageStudyTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val backStack = rememberNavBackStack(initialKey = NavRoute.Portfolio)
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val useNavRail = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val provider = remember {
        entryProvider {
            entry<NavRoute.Portfolio> { PortfolioScreen() }
            entry<NavRoute.Vocab> { VocabScreen() }
            entry<NavRoute.Skills> { SkillsScreen() }
            entry<NavRoute.Journal> { JournalScreen() }
            entry<NavRoute.Admin> { AdminScreen() }
        }
    }

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
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
            if (useNavRail) {
                NavigationRail {
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
                backstack = backStack,
                entryProvider = provider,
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
