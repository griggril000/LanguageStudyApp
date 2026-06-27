package io.github.languagestudy.ui.screens

import androidx.activity.compose.BackHandler
import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.languagestudy.navigation.NavRoute
import io.github.languagestudy.navigation.label
import io.github.languagestudy.ui.auth.AuthViewModel
import io.github.languagestudy.ui.components.DeleteConfirmationDialog
import io.github.languagestudy.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by authViewModel.user.collectAsState()
    val isMentorMode by authViewModel.isMentorMode.collectAsState()
    val userSettings by settingsViewModel.userSettings.collectAsState()
    val mentorCode by settingsViewModel.mentorCode.collectAsState()
    val availableLanguages by settingsViewModel.availableLanguages.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrimaryLangDialog by remember { mutableStateOf(false) }
    var showLearningLangsDialog by remember { mutableStateOf(false) }
    var showStartupTabDialog by remember { mutableStateOf(false) }
    var showAccessLevelDialog by remember { mutableStateOf(false) }
    
    var currentView by remember { mutableStateOf("main") }

    BackHandler(enabled = true) {
        if (currentView != "main") {
            currentView = "main"
        } else {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.errorMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = "Delete Account?",
            message = "This will permanently delete your account and all your data (vocabulary, skills, portfolio, journal, etc.). This action cannot be undone.",
            onConfirm = {
                authViewModel.deleteAccount(context) { success, message ->
                    if (!success && message != null) {
                        scope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    }
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showJoinDialog) {
        JoinMentorDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                scope.launch {
                    try {
                        val ownerUid = settingsViewModel.validateCode(code)
                        if (ownerUid != null) {
                            if (ownerUid == currentUser?.uid) {
                                snackbarHostState.showSnackbar("Cannot mentor yourself")
                            } else {
                                authViewModel.enterMentorMode(ownerUid, code)
                                showJoinDialog = false
                            }
                        } else {
                            snackbarHostState.showSnackbar("Invalid or disabled code")
                        }
                    } catch (_: Exception) {
                        snackbarHostState.showSnackbar("Error validating code")
                    }
                }
            }
        )
    }

    // Dialogs for selections
    if (showThemeDialog) {
        SelectionDialog(
            title = "Select Theme",
            options = listOf("system", "light", "dark"),
            selectedOption = userSettings.theme,
            onOptionSelected = { settingsViewModel.setTheme(it) },
            onDismiss = { showThemeDialog = false },
            labelProvider = { it.replaceFirstChar { char -> char.uppercase() } }
        )
    }

    if (showPrimaryLangDialog) {
        SelectionDialog(
            title = "Primary Language",
            options = userSettings.learnedLanguages,
            selectedOption = userSettings.languageLearning,
            onOptionSelected = { settingsViewModel.setCurrentLanguage(it) },
            onDismiss = { showPrimaryLangDialog = false }
        )
    }

    if (showLearningLangsDialog) {
        MultiSelectionDialog(
            title = "Languages I'm Learning",
            options = availableLanguages,
            selectedOptions = userSettings.learnedLanguages,
            onToggleOption = { settingsViewModel.toggleLanguage(it) },
            onDismiss = { showLearningLangsDialog = false }
        )
    }

    if (showStartupTabDialog) {
        SelectionDialog(
            title = "Startup Tab",
            options = NavRoute.mainRoutes,
            selectedOption = NavRoute.mainRoutes.find { it.label.lowercase() == userSettings.homepageTab } ?: NavRoute.Vocab,
            onOptionSelected = { settingsViewModel.setHomepageTab(it.label.lowercase()) },
            onDismiss = { showStartupTabDialog = false },
            labelProvider = { it.label }
        )
    }

    if (showAccessLevelDialog) {
        SelectionDialog(
            title = "Mentor Access Level",
            options = listOf("view", "status", "full"),
            selectedOption = userSettings.mentorAccessLevel,
            onOptionSelected = { settingsViewModel.setMentorAccessLevel(it) },
            onDismiss = { showAccessLevelDialog = false },
            labelProvider = { 
                when(it) {
                    "view" -> "Read Only"
                    "status" -> "Status Updates"
                    "full" -> "Edit All"
                    else -> it.replaceFirstChar { char -> char.uppercase() }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(when(currentView) {
                        "details" -> "App Details"
                        "notes" -> "Release Notes"
                        "libraries" -> "Libraries Used"
                        else -> "Settings"
                    })
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (currentView != "main") {
                            currentView = "main"
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentView) {
                "main" -> SettingsMainView(
                    currentUser = currentUser,
                    isMentorMode = isMentorMode,
                    userSettings = userSettings,
                    mentorCode = mentorCode,
                    settingsViewModel = settingsViewModel,
                    authViewModel = authViewModel,
                    onNavigateToDetails = { currentView = "details" },
                    onNavigateToNotes = { currentView = "notes" },
                    onNavigateToLibraries = { currentView = "libraries" },
                    onShowDeleteDialog = { showDeleteDialog = true },
                    onShowJoinDialog = { showJoinDialog = true },
                    onShowThemeDialog = { showThemeDialog = true },
                    onShowPrimaryLangDialog = { showPrimaryLangDialog = true },
                    onShowLearningLangsDialog = { showLearningLangsDialog = true },
                    onShowStartupTabDialog = { showStartupTabDialog = true },
                    onShowAccessLevelDialog = { showAccessLevelDialog = true }
                )

                "details" -> AppDetailsView()
                "notes" -> ReleaseNotesView()
                "libraries" -> LibrariesView()
            }
        }
    }
}

@Composable
fun SettingsMainView(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isMentorMode: Boolean,
    userSettings: io.github.languagestudy.data.model.UserSettings,
    mentorCode: String?,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    onNavigateToDetails: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToLibraries: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    onShowJoinDialog: () -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowPrimaryLangDialog: () -> Unit,
    onShowLearningLangsDialog: () -> Unit,
    onShowStartupTabDialog: () -> Unit,
    onShowAccessLevelDialog: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        PreferenceCategory(title = "Account")
        PreferenceItem(
            title = "Signed in as",
            summary = currentUser?.email ?: "Not signed in",
            icon = Icons.Default.Person,
            onClick = {}
        )
        PreferenceItem(
            title = "Sign Out",
            icon = Icons.AutoMirrored.Filled.Logout,
            onClick = { authViewModel.signOut(context) }
        )
        if (currentUser != null && !isMentorMode) {
            PreferenceItem(
                title = "Delete Account",
                icon = Icons.Default.DeleteForever,
                onClick = onShowDeleteDialog,
                isError = true
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = "Display")
        PreferenceItem(
            title = "Theme",
            summary = userSettings.theme.replaceFirstChar { it.uppercase() },
            icon = Icons.Default.Palette,
            onClick = onShowThemeDialog
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = "Learning")
        PreferenceItem(
            title = "Primary Language",
            summary = userSettings.languageLearning.ifBlank { "None" },
            icon = Icons.Default.Done,
            onClick = onShowPrimaryLangDialog
        )
        PreferenceItem(
            title = "Languages I'm Learning",
            summary = if (userSettings.learnedLanguages.isEmpty()) "None" else userSettings.learnedLanguages.joinToString(", "),
            icon = Icons.Default.Language,
            onClick = onShowLearningLangsDialog
        )
        PreferenceItem(
            title = "Startup Tab",
            summary = userSettings.homepageTab.replaceFirstChar { it.uppercase() },
            icon = Icons.Rounded.Home,
            onClick = onShowStartupTabDialog
        )
        if (!isMentorMode) {
            PreferenceItem(
                title = "Request a Language",
                summary = "Don't see your language? Let us know.",
                icon = Icons.Default.Language,
                onClick = {
                    val uri = "mailto:griggriley+languagestudy@gmail.com" +
                            "?subject=${android.net.Uri.encode("Language Study - New Language Request")}" +
                            "&body=${android.net.Uri.encode("I would like to request support for the following language: ")}"
                    try {
                        uriHandler.openUri(uri)
                    } catch (_: Exception) {
                    }
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = "Mentor Access")
        SwitchPreference(
            title = "Enable Mentor View",
            summary = if (isMentorMode) "Settings are read-only in mentor mode." else "Allow someone with your code to view your progress.",
            icon = Icons.Default.SupervisorAccount,
            checked = userSettings.mentorCodeEnabled,
            onCheckedChange = { settingsViewModel.toggleMentorCode(it) },
            enabled = !isMentorMode
        )

        if (userSettings.mentorCodeEnabled || isMentorMode) {
            PreferenceItem(
                title = "Mentor Share Code",
                summary = mentorCode ?: "Generating...",
                icon = Icons.Rounded.Refresh,
                onClick = {
                    mentorCode?.let { code ->
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        "Mentor Code",
                                        code
                                    )
                                )
                            )
                        }
                    }
                },
                trailing = {
                    if (!isMentorMode) {
                        IconButton(onClick = { settingsViewModel.regenerateMentorCode() }) {
                            Icon(
                                Icons.Rounded.Refresh,
                                contentDescription = "Regenerate",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
            PreferenceItem(
                title = "Mentor Access Level",
                summary = userSettings.mentorAccessLevel.replaceFirstChar { it.uppercase() },
                icon = Icons.Default.SupervisorAccount,
                onClick = onShowAccessLevelDialog
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = "Mentoring")
        if (isMentorMode) {
            PreferenceItem(
                title = "Currently Mentoring",
                summary = "You are viewing another user's progress. Click to exit.",
                icon = Icons.Default.School,
                onClick = { authViewModel.exitMentorMode() },
                trailing = {
                    TextButton(onClick = { authViewModel.exitMentorMode() }) {
                        Text("Exit")
                    }
                }
            )
        } else {
            PreferenceItem(
                title = "Mentor a Student",
                summary = "Enter a share code to view their progress.",
                icon = Icons.Default.School,
                onClick = onShowJoinDialog
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = "Additional Info")
        PreferenceItem(
            title = "Release Notes",
            onClick = onNavigateToNotes
        )
        PreferenceItem(
            title = "App Details",
            onClick = onNavigateToDetails
        )
        PreferenceItem(
            title = "Libraries Used",
            onClick = onNavigateToLibraries
        )
        PreferenceItem(
            title = "Credits",
            summary = "Globe icons by Techno Icons - Flaticon",
            onClick = { uriHandler.openUri("https://www.flaticon.com/free-icons/globe") }
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AppDetailsView() {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailItem(label = "Language Study Version", value = packageInfo?.versionName ?: "1.0.0")
        DetailItem(label = "Android Version", value = android.os.Build.VERSION.RELEASE)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

        Text(
            "Personal Data",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Placeholders for data counts - in a real app these would come from ViewModels
        DetailItem(label = "Vocabulary items", value = "...")
        DetailItem(label = "Skills tracked", value = "...")
        DetailItem(label = "Portfolio entries", value = "...")
        DetailItem(label = "Journal entries", value = "...")
    }
}

@Composable
fun ReleaseNotesView() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Version 1.2.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("• Redesigned Settings screen with a cleaner layout.\n" +
                "• Added Mentor View functionality for progress tracking.\n" +
                "• Improved vocabulary and skill management.\n" +
                "• New Portfolio and Journal features for immersive learning.",
            style = MaterialTheme.typography.bodyLarge)
        
        HorizontalDivider()
        
        Text("Version 1.1.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("• Initial release with core features.\n" +
                "• Support for multiple languages.\n" +
                "• Dark mode support.",
            style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun LibrariesView() {
    val scrollState = rememberScrollState()
    val libraries = listOf(
        "Jetpack Compose" to "Modern toolkit for building native UI.",
        "Firebase" to "Authentication, Firestore, and Cloud Messaging.",
        "Room" to "Persistence library for local database.",
        "Retrofit & OkHttp" to "Type-safe HTTP client for Android and Java.",
        "Coil" to "Image loading library for Android backed by Kotlin Coroutines.",
        "YouTube Player" to "Official YouTube Player API for Android.",
        "Navigation 3" to "Declarative navigation for Compose.",
        "Moshi" to "A modern JSON library for Android and Java.",
        "Kotlin Coroutines" to "Asynchronous programming made easy."
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        libraries.forEach { (name, desc) ->
            Column {
                Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
    }
}

@Composable
fun JoinMentorDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mentor a Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter the 5-character share code from the student's app.")
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 5) code = it.uppercase() },
                    label = { Text("Share Code") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = code.length == 5,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Join")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    onDismiss: () -> Unit,
    labelProvider: (T) -> String = { it.toString() }
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOptionSelected(option)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option == selectedOption),
                            onClick = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(labelProvider(option))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun MultiSelectionDialog(
    title: String,
    options: List<String>,
    selectedOptions: List<String>,
    onToggleOption: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEach { option ->
                    val isSelected = selectedOptions.contains(option)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleOption(option) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(option)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    onClick: () -> Unit = {},
    isError: Boolean = false,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun SwitchPreference(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    PreferenceItem(
        title = title,
        summary = summary,
        icon = icon,
        onClick = { if (enabled) onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}
