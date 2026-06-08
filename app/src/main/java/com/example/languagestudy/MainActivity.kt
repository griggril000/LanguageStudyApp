package com.example.languagestudy

import android.os.Bundle
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val backStack = rememberNavBackStack(NavRoute.Portfolio as NavKey)
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val useNavRail = adaptiveInfo.windowSizeClass.windowWidthSizeClass != WindowWidthSizeClass.COMPACT

    val provider = remember {
        entryProvider<NavKey> {
            entry<NavRoute.Portfolio> { PortfolioScreen() }
            entry<NavRoute.Vocab> { VocabScreen() }
            entry<NavRoute.Skills> { SkillsScreen() }
            entry<NavRoute.Journal> { JournalScreen() }
            entry<NavRoute.Admin> { AdminScreen() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Language Study",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "Logged in as: user@example.com",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Settings")
                        }
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFAB91)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Logout", color = Color.Black)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
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
