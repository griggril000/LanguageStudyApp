package io.github.langstudy.ui.screens

import android.content.ClipData
import android.content.pm.PackageManager
import android.os.Build
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import io.github.langstudy.BuildConfig
import io.github.langstudy.R
import io.github.langstudy.navigation.NavRoute
import io.github.langstudy.navigation.label
import io.github.langstudy.ui.auth.AuthViewModel
import io.github.langstudy.ui.components.AccountManagementDialog
import io.github.langstudy.ui.components.ContactDialog
import io.github.langstudy.ui.components.DeleteConfirmationDialog
import io.github.langstudy.ui.components.QRCodeScanner
import io.github.langstudy.ui.components.UpdateEmailDialog
import io.github.langstudy.ui.viewmodel.SettingsViewModel
import io.github.langstudy.utils.QRCodeGenerator
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
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
    val userMentorCode by settingsViewModel.mentorCode.collectAsState()
    val sessionMentorCode by authViewModel.mentorCode.collectAsState()
    val mentorCode = if (isMentorMode) sessionMentorCode else userMentorCode

    val availableLanguages by settingsViewModel.availableLanguages.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showPrimaryLangDialog by remember { mutableStateOf(false) }
    var showLearningLangsDialog by remember { mutableStateOf(false) }
    var showStartupTabDialog by remember { mutableStateOf(false) }
    var showAccessLevelDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showAccountOptionsDialog by remember { mutableStateOf(false) }
    var showUpdateEmailDialog by remember { mutableStateOf(false) }
    var showQRCodeDialog by remember { mutableStateOf(false) }
    var showQRScannerDialog by remember { mutableStateOf(false) }

    var currentView by remember { mutableStateOf("main") }

    val mainScrollState = rememberScrollState()
    val detailsScrollState = rememberScrollState()
    val notesScrollState = rememberScrollState()
    val librariesLazyListState = rememberLazyListState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                showQRScannerDialog = true
            } else {
                makeText(
                    context,
                    context.getString(R.string.camera_permission_denied),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )

    val handleShowQRScanner = {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showQRScannerDialog = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val handleBack = {
        when (currentView) {
            "notes" -> currentView = "details"
            "details", "libraries" -> currentView = "main"
            else -> onBack()
        }
    }

    BackHandler(enabled = true, onBack = handleBack)

    LaunchedEffect(Unit) {
        settingsViewModel.errorMessages.collect { message ->
            makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.error.collect { message ->
            message?.let {
                makeText(context, it, Toast.LENGTH_SHORT).show()
                authViewModel.clearError()
            }
        }
    }

    if (showAccountOptionsDialog && currentUser != null) {
        AccountManagementDialog(
            email = currentUser?.email,
            onDismiss = { showAccountOptionsDialog = false },
            onUpdateEmailClick = {
                showAccountOptionsDialog = false
                showUpdateEmailDialog = true
            },
            onResetPasswordClick = {
                showAccountOptionsDialog = false
                currentUser?.email?.let { email ->
                    authViewModel.resetPassword(email)
                }
            }
        )
    }

    if (showUpdateEmailDialog) {
        val verificationSent = stringResource(R.string.verification_email_sent)
        val failedUpdate = stringResource(R.string.failed_update_email)
        UpdateEmailDialog(
            currentEmail = currentUser?.email,
            onDismiss = { showUpdateEmailDialog = false },
            onConfirm = { newEmail ->
                authViewModel.updateEmail(newEmail) { success, message ->
                    makeText(
                        context,
                        message ?: (if (success) verificationSent else failedUpdate),
                        Toast.LENGTH_LONG
                    ).show()
                    if (success) showUpdateEmailDialog = false
                }
            }
        )
    }

    if (showQRCodeDialog && mentorCode != null) {
        val qrCodeUrl = "https://language-study.github.io/?mentor=$mentorCode"
        val qrBitmap = remember(qrCodeUrl) { QRCodeGenerator.generateQRCode(qrCodeUrl, 512) }

        AlertDialog(
            onDismissRequest = { showQRCodeDialog = false },
            title = { Text(stringResource(R.string.mentor_share_qr)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mentor_qr_info),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (qrBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .size(200.dp)
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(8.dp)
                        )
                    }
                    Text(
                        text = mentorCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showQRCodeDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    if (showQRScannerDialog) {
        val cannotMentorSelf = stringResource(R.string.cannot_mentor_yourself)
        val invalidCode = stringResource(R.string.invalid_disabled_code)
        val errorValidating = stringResource(R.string.error_validating_code)

        var isProcessing by remember { mutableStateOf(false) }
        var lastScannedCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQRScannerDialog = false },
            title = { Text(stringResource(R.string.scan_mentor_qr)) },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(androidx.compose.ui.graphics.Color.Black)
                ) {
                    QRCodeScanner { scannedValue ->
                        if (isProcessing || scannedValue == lastScannedCode) return@QRCodeScanner

                        val code = try {
                            val uri = android.net.Uri.parse(scannedValue)
                            val mentorFromUri = if (uri.scheme != null) {
                                uri.getQueryParameter("mentor")
                                    ?: uri.getQueryParameter("returnTo")?.let {
                                        android.net.Uri.parse(it).getQueryParameter("mentor")
                                    }
                            } else if (scannedValue.contains("mentor=")) {
                                android.net.Uri.parse("https://$scannedValue")
                                    .getQueryParameter("mentor")
                            } else {
                                null
                            }
                            mentorFromUri ?: scannedValue
                        } catch (e: Exception) {
                            scannedValue
                        }

                        // Only proceed if it looks like a 5-char code, not a full URL we failed to parse
                        if (code.length == 5 || (code.length > 5 && !code.contains("/") && !code.contains(
                                "."
                            ))
                        ) {
                            val sanitizedCode = code.take(5).uppercase()
                            if (sanitizedCode == lastScannedCode) return@QRCodeScanner

                            isProcessing = true
                            lastScannedCode = sanitizedCode

                            scope.launch {
                                try {
                                    val ownerUid = settingsViewModel.validateCode(sanitizedCode)
                                    if (ownerUid != null) {
                                        if (ownerUid == currentUser?.uid) {
                                            makeText(
                                                context,
                                                cannotMentorSelf,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            authViewModel.enterMentorMode(
                                                context,
                                                ownerUid,
                                                sanitizedCode
                                            )
                                            showQRScannerDialog = false
                                        }
                                    } else {
                                        makeText(context, invalidCode, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                } catch (_: Exception) {
                                    makeText(context, errorValidating, Toast.LENGTH_SHORT)
                                        .show()
                                } finally {
                                    isProcessing = false
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQRScannerDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_account_title),
            message = stringResource(R.string.delete_account_message),
            onConfirm = {
                authViewModel.deleteAccount(context) { success, message ->
                    if (!success && message != null) {
                        makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showJoinDialog) {
        val cannotMentorSelf = stringResource(R.string.cannot_mentor_yourself)
        val invalidCode = stringResource(R.string.invalid_disabled_code)
        val errorValidating = stringResource(R.string.error_validating_code)

        JoinMentorDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { code ->
                scope.launch {
                    try {
                        val ownerUid = settingsViewModel.validateCode(code)
                        if (ownerUid != null) {
                            if (ownerUid == currentUser?.uid) {
                                makeText(context, cannotMentorSelf, Toast.LENGTH_SHORT).show()
                            } else {
                                authViewModel.enterMentorMode(context, ownerUid, code)
                                showJoinDialog = false
                            }
                        } else {
                            makeText(context, invalidCode, Toast.LENGTH_SHORT).show()
                        }
                    } catch (_: Exception) {
                        makeText(context, errorValidating, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onScan = {
                showJoinDialog = false
                handleShowQRScanner()
            }
        )
    }

    // Dialogs for selections
    if (showThemeDialog) {
        val themeOptions = remember {
            val list = mutableListOf("system", "light", "dark")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                list.add("dynamic")
            }
            list
        }
        val systemDefault = stringResource(R.string.system_default)
        val dynamicColor = stringResource(R.string.dynamic_color)
        SelectionDialog(
            title = stringResource(R.string.select_theme),
            options = themeOptions,
            selectedOption = userSettings.theme,
            onOptionSelected = { settingsViewModel.setTheme(it) },
            onDismiss = { showThemeDialog = false },
            labelProvider = {
                when (it) {
                    "system" -> systemDefault
                    "dynamic" -> dynamicColor
                    else -> it.replaceFirstChar { char -> char.uppercase() }
                }
            }
        )
    }

    if (showPrimaryLangDialog) {
        SelectionDialog(
            title = stringResource(R.string.primary_language_title),
            options = userSettings.learnedLanguages,
            selectedOption = userSettings.languageLearning,
            onOptionSelected = { settingsViewModel.setCurrentLanguage(it) },
            onDismiss = { showPrimaryLangDialog = false }
        )
    }

    if (showLearningLangsDialog) {
        MultiSelectionDialog(
            title = stringResource(R.string.learning_langs_title),
            options = availableLanguages,
            selectedOptions = userSettings.learnedLanguages,
            onToggleOption = { settingsViewModel.toggleLanguage(it) },
            onDismiss = { showLearningLangsDialog = false }
        )
    }

    if (showStartupTabDialog) {
        SelectionDialog(
            title = stringResource(R.string.startup_tab_title),
            options = NavRoute.mainRoutes,
            selectedOption = NavRoute.mainRoutes.find { it.label.lowercase() == userSettings.homepageTab }
                ?: NavRoute.Vocab,
            onOptionSelected = { settingsViewModel.setHomepageTab(it.label.lowercase()) },
            onDismiss = { showStartupTabDialog = false },
            labelProvider = { it.label }
        )
    }

    if (showAccessLevelDialog) {
        val readOnly = stringResource(R.string.read_only)
        val statusUpdates = stringResource(R.string.status_updates)
        val editAll = stringResource(R.string.edit_all)

        SelectionDialog(
            title = stringResource(R.string.mentor_access_level_title),
            options = listOf("view", "status", "full"),
            selectedOption = userSettings.mentorAccessLevel,
            onOptionSelected = { settingsViewModel.setMentorAccessLevel(it) },
            onDismiss = { showAccessLevelDialog = false },
            labelProvider = {
                when (it) {
                    "view" -> readOnly
                    "status" -> statusUpdates
                    "full" -> editAll
                    else -> it.replaceFirstChar { char -> char.uppercase() }
                }
            }
        )
    }

    if (showContactDialog) {
        ContactDialog(
            onDismiss = { showContactDialog = false },
            onSubmit = { type, message, resName, resLoc ->
                settingsViewModel.submitContactRequest(
                    type = type.name,
                    message = message,
                    resourceName = resName,
                    resourceLocation = resLoc,
                    email = currentUser?.email,
                    isMentorMode = isMentorMode
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentView) {
                            "details" -> stringResource(R.string.app_details)
                            "notes" -> stringResource(R.string.release_notes)
                            "libraries" -> stringResource(R.string.libraries_used)
                            else -> stringResource(R.string.settings_title)
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_cd)
                        )
                    }
                }
            )
        },
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
                    scrollState = mainScrollState,
                    onNavigateToDetails = { currentView = "details" },
                    onNavigateToLibraries = { currentView = "libraries" },
                    onShowDeleteDialog = { showDeleteDialog = true },
                    onShowJoinDialog = { showJoinDialog = true },
                    onShowThemeDialog = { showThemeDialog = true },
                    onShowPrimaryLangDialog = { showPrimaryLangDialog = true },
                    onShowLearningLangsDialog = { showLearningLangsDialog = true },
                    onShowStartupTabDialog = { showStartupTabDialog = true },
                    onShowAccessLevelDialog = { showAccessLevelDialog = true },
                    onShowContactDialog = { showContactDialog = true },
                    onShowAccountOptionsDialog = { showAccountOptionsDialog = true },
                    onShowQRCodeDialog = { showQRCodeDialog = true },
                    onShowQRScannerDialog = handleShowQRScanner
                )

                "details" -> AppDetailsView(
                    settingsViewModel = settingsViewModel,
                    scrollState = detailsScrollState,
                    isMentorMode = isMentorMode,
                    onNavigateToNotes = { currentView = "notes" }
                )

                "notes" -> {
                    val releaseNotes by settingsViewModel.releaseNotes.collectAsState()
                    ReleaseNotesView(
                        releaseNotes = releaseNotes,
                        scrollState = notesScrollState
                    )
                }

                "libraries" -> LibrariesView(lazyListState = librariesLazyListState)
            }
        }
    }
}

@Composable
fun SettingsMainView(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    isMentorMode: Boolean,
    userSettings: io.github.langstudy.data.model.UserSettings,
    mentorCode: String?,
    settingsViewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    scrollState: ScrollState,
    onNavigateToDetails: () -> Unit,
    onNavigateToLibraries: () -> Unit,
    onShowDeleteDialog: () -> Unit,
    onShowJoinDialog: () -> Unit,
    onShowThemeDialog: () -> Unit,
    onShowPrimaryLangDialog: () -> Unit,
    onShowLearningLangsDialog: () -> Unit,
    onShowStartupTabDialog: () -> Unit,
    onShowAccessLevelDialog: () -> Unit,
    onShowContactDialog: () -> Unit,
    onShowAccountOptionsDialog: () -> Unit,
    onShowQRCodeDialog: () -> Unit,
    onShowQRScannerDialog: () -> Unit
) {
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
        if (isMentorMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.mentor_view_info),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        PreferenceCategory(title = stringResource(R.string.pref_learning))
        PreferenceItem(
            title = stringResource(R.string.pref_primary_lang),
            summary = userSettings.languageLearning.ifBlank { stringResource(R.string.none) },
            icon = Icons.Default.Done,
            onClick = if (isMentorMode) ({}) else onShowPrimaryLangDialog
        )
        PreferenceItem(
            title = stringResource(R.string.pref_learning_langs),
            summary = if (userSettings.learnedLanguages.isEmpty()) stringResource(R.string.none) else userSettings.learnedLanguages.joinToString(
                ", "
            ),
            icon = Icons.Default.Language,
            onClick = if (isMentorMode) ({}) else onShowLearningLangsDialog
        )
        PreferenceItem(
            title = stringResource(R.string.pref_startup_tab),
            summary = userSettings.homepageTab.replaceFirstChar { it.uppercase() },
            icon = Icons.Rounded.Home,
            onClick = if (isMentorMode) ({}) else onShowStartupTabDialog
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = stringResource(R.string.pref_display))
        val systemDefault = stringResource(R.string.system_default)
        val dynamicColor = stringResource(R.string.dynamic_color)
        PreferenceItem(
            title = stringResource(R.string.pref_theme),
            summary = when (userSettings.theme) {
                "system" -> systemDefault
                "dynamic" -> dynamicColor
                else -> userSettings.theme.replaceFirstChar { it.uppercase() }
            },
            icon = Icons.Default.Palette,
            onClick = if (isMentorMode) ({}) else onShowThemeDialog
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = stringResource(R.string.pref_mentor_access))
        SwitchPreference(
            title = stringResource(R.string.enable_mentor_view),
            summary = if (isMentorMode) stringResource(R.string.enable_mentor_view_mentor_summary) else stringResource(
                R.string.enable_mentor_view_summary
            ),
            icon = Icons.Default.SupervisorAccount,
            checked = userSettings.mentorCodeEnabled,
            onCheckedChange = { settingsViewModel.toggleMentorCode(it) },
            enabled = !isMentorMode
        )

        if (userSettings.mentorCodeEnabled || isMentorMode) {
            val generating = stringResource(R.string.generating)
            val mentorCodeText = stringResource(R.string.mentor_code_cd)
            PreferenceItem(
                title = if (isMentorMode) stringResource(R.string.student_share_code) else stringResource(
                    R.string.mentor_share_code
                ),
                summary = mentorCode ?: generating,
                icon = Icons.Rounded.Refresh,
                onClick = {
                    mentorCode?.let { code ->
                        scope.launch {
                            clipboard.setClipEntry(
                                ClipEntry(
                                    ClipData.newPlainText(
                                        mentorCodeText,
                                        code
                                    )
                                )
                            )
                        }
                    }
                },
                trailing = {
                    Row {
                        IconButton(onClick = onShowQRCodeDialog) {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = stringResource(R.string.mentor_share_qr),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (!isMentorMode) {
                            IconButton(onClick = { settingsViewModel.regenerateMentorCode() }) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.regenerate_cd),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            )
            val readOnly = stringResource(R.string.read_only)
            val statusUpdates = stringResource(R.string.status_updates)
            val editAll = stringResource(R.string.edit_all)
            PreferenceItem(
                title = if (isMentorMode) stringResource(R.string.student_access_level) else stringResource(
                    R.string.mentor_access_level
                ),
                summary = when (userSettings.mentorAccessLevel) {
                    "view" -> readOnly
                    "status" -> statusUpdates
                    "full" -> editAll
                    else -> userSettings.mentorAccessLevel.replaceFirstChar { it.uppercase() }
                },
                icon = Icons.Default.SupervisorAccount,
                onClick = if (isMentorMode) ({}) else onShowAccessLevelDialog
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = stringResource(R.string.pref_mentoring))
        if (isMentorMode) {
            PreferenceItem(
                title = stringResource(R.string.currently_mentoring),
                summary = stringResource(R.string.currently_mentoring_summary),
                icon = Icons.Default.School,
                onClick = { authViewModel.exitMentorMode(context) },
                trailing = {
                    TextButton(onClick = { authViewModel.exitMentorMode(context) }) {
                        Text(stringResource(R.string.exit))
                    }
                }
            )
        } else {
            PreferenceItem(
                title = stringResource(R.string.mentor_student_title),
                summary = stringResource(R.string.mentor_a_student_summary),
                icon = Icons.Default.School,
                onClick = onShowJoinDialog,
                trailing = {
                    IconButton(onClick = onShowQRScannerDialog) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = stringResource(R.string.scan_mentor_qr),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = stringResource(R.string.pref_account))
        PreferenceItem(
            title = if (isMentorMode) stringResource(R.string.mentoring_student) else stringResource(
                R.string.signed_in_as
            ),
            summary = if (isMentorMode) stringResource(
                R.string.mentor_code_format,
                mentorCode ?: ""
            ) else currentUser?.email ?: stringResource(R.string.not_signed_in),
            icon = Icons.Default.Person,
            onClick = {
                if (!isMentorMode && currentUser != null) {
                    onShowAccountOptionsDialog()
                }
            }
        )
        PreferenceItem(
            title = stringResource(R.string.sign_out),
            icon = Icons.AutoMirrored.Filled.Logout,
            onClick = { authViewModel.signOut(context) }
        )
        if (currentUser != null && !isMentorMode) {
            PreferenceItem(
                title = stringResource(R.string.delete_account),
                icon = Icons.Default.DeleteForever,
                onClick = onShowDeleteDialog,
                isError = true
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

        PreferenceCategory(title = stringResource(R.string.pref_additional_info))
        if (!isMentorMode) {
            PreferenceItem(
                title = stringResource(R.string.pref_contact),
                summary = stringResource(R.string.pref_contact_summary),
                icon = Icons.Default.Feedback,
                onClick = onShowContactDialog
            )
        }
        PreferenceItem(
            title = stringResource(R.string.app_details),
            summary = stringResource(R.string.app_details_version_format, BuildConfig.VERSION_NAME),
            onClick = onNavigateToDetails
        )
        PreferenceItem(
            title = stringResource(R.string.libraries_used),
            onClick = onNavigateToLibraries
        )
        PreferenceItem(
            title = stringResource(R.string.credits),
            summary = stringResource(R.string.credits_summary),
            onClick = { uriHandler.openUri("https://www.flaticon.com/free-icons/globe") }
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AppDetailsView(
    settingsViewModel: SettingsViewModel,
    scrollState: ScrollState,
    isMentorMode: Boolean = false,
    onNavigateToNotes: () -> Unit
) {
    val vocabCount by settingsViewModel.vocabCount.collectAsState()
    val skillCount by settingsViewModel.skillCount.collectAsState()
    val portfolioCount by settingsViewModel.portfolioCount.collectAsState()
    val journalCount by settingsViewModel.journalCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DetailItem(
            label = stringResource(R.string.lang_study_version_label),
            value = BuildConfig.VERSION_NAME
        )
        PreferenceItem(
            title = stringResource(R.string.release_notes),
            summary = stringResource(R.string.release_notes_summary),
            onClick = onNavigateToNotes
        )
        DetailItem(
            label = stringResource(R.string.android_version_label),
            value = android.os.Build.VERSION.RELEASE
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

        if (!isMentorMode) {
            Text(
                text = stringResource(R.string.personal_data),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            DetailItem(
                label = stringResource(R.string.vocab_items_label),
                value = vocabCount.toString()
            )
            DetailItem(
                label = stringResource(R.string.skills_tracked_label),
                value = skillCount.toString()
            )
            DetailItem(
                label = stringResource(R.string.portfolio_entries_label),
                value = portfolioCount.toString()
            )
            DetailItem(
                label = stringResource(R.string.journal_entries_label),
                value = journalCount.toString()
            )
        }
    }
}

@Composable
fun ReleaseNotesView(
    releaseNotes: List<io.github.langstudy.data.model.GitHubRelease>,
    scrollState: ScrollState
) {
    val currentVersion = BuildConfig.VERSION_NAME

    val filteredReleases = remember(releaseNotes) {
        releaseNotes.filter { release ->
            val tag = release.tagName.removePrefix("v")
            compareVersions(tag, currentVersion) <= 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (filteredReleases.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (releaseNotes.isEmpty()) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.no_release_notes))
                }
            }
        } else {
            filteredReleases.forEach { release ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                release.name ?: release.tagName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            release.publishedAt?.let { dateStr ->
                                formatDate(dateStr)?.let { dateText ->
                                    Text(
                                        dateText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 4.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        release.body?.let {
                            MarkdownText(
                                markdown = it,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val markwon = remember { Markwon.create(context) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 16f
                setLinkTextColor(linkColor)
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            markwon.setMarkdown(textView, markdown)
            android.text.util.Linkify.addLinks(textView, android.text.util.Linkify.WEB_URLS)
        }
    )
}

private fun formatDate(isoString: String): String? {
    return try {
        // GitHub API returns ISO 8601 format like "2023-10-27T12:34:56Z"
        val inputFormat =
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        inputFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val date = inputFormat.parse(isoString)
        val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        date?.let { outputFormat.format(it) }
    } catch (e: Exception) {
        null
    }
}

private fun compareVersions(v1: String, v2: String): Int {
    val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
    val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
    val length = maxOf(parts1.size, parts2.size)
    for (i in 0 until length) {
        val p1 = if (i < parts1.size) parts1[i] else 0
        val p2 = if (i < parts2.size) parts2[i] else 0
        if (p1 < p2) return -1
        if (p1 > p2) return 1
    }
    return 0
}

@Composable
fun LibrariesView(lazyListState: LazyListState) {
    val libraries by produceLibraries(io.github.langstudy.R.raw.aboutlibraries)

    LibrariesContainer(
        libraries = libraries,
        modifier = Modifier.fillMaxSize(),
        lazyListState = lazyListState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    )
}

@Composable
fun JoinMentorDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit,
    onScan: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.mentor_student_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.enter_share_code))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 5) code = it.uppercase() },
                    label = { Text(stringResource(R.string.share_code_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onScan) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = stringResource(R.string.scan_qr)
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onJoin(code) },
                enabled = code.length == 5,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.join))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
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
            Button(onClick = onDismiss) { Text(stringResource(R.string.done)) }
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
                enabled = enabled,
                thumbContent = {
                    Icon(
                        imageVector = if (checked) Icons.Rounded.Check else Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                    )
                },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedIconColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    )
}
