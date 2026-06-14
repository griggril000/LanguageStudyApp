package com.example.languagestudy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.ui.auth.AuthViewModel
import com.example.languagestudy.ui.components.SectionHeader
import com.example.languagestudy.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentUser by authViewModel.user.collectAsState()
    val isMentorMode by authViewModel.isMentorMode.collectAsState()
    val userSettings by settingsViewModel.userSettings.collectAsState()
    val availableLanguages by settingsViewModel.availableLanguages.collectAsState()
    val mentorCode by settingsViewModel.mentorCode.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (currentUser != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Column {
                        Text(
                            text = if (isMentorMode) "Viewing Account" else "Account",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(text = currentUser?.email ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Languages you are learning", icon = Icons.Default.Language)
            if (availableLanguages.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableLanguages.forEach { language ->
                        val isSelected = userSettings.learnedLanguages.contains(language)
                        val canToggle = (!isSelected || userSettings.learnedLanguages.size > 1) && !isMentorMode
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (canToggle) settingsViewModel.toggleLanguage(language) },
                            enabled = canToggle || isSelected,
                            label = { Text(language) },
                            leadingIcon = if (isSelected) {
                                { Icon(imageVector = Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }
            }
        }

        if (userSettings.learnedLanguages.isNotEmpty()) {
            HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Primary Language for Study", icon = Icons.Default.Done)
                Text(text = "Select which language to focus on.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    userSettings.learnedLanguages.forEach { language ->
                        val isSelected = userSettings.languageLearning == language
                        val canChange = !isMentorMode && userSettings.learnedLanguages.size > 1
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (canChange) settingsViewModel.setCurrentLanguage(language) },
                            enabled = canChange || isSelected,
                            label = { Text(language) },
                            leadingIcon = if (isSelected) {
                                { Icon(imageVector = Icons.Rounded.RadioButtonChecked, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                            } else null
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(title = "Mentor Access", icon = Icons.Default.SupervisorAccount)
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Enable Mentor View", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (isMentorMode) "Settings are read-only in mentor mode." else "Allow someone with your code to view your progress.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = userSettings.mentorCodeEnabled,
                    onCheckedChange = { settingsViewModel.toggleMentorCode(it) },
                    enabled = !isMentorMode,
                    thumbContent = {
                        val icon = if (userSettings.mentorCodeEnabled) Icons.Rounded.Check else Icons.Rounded.Close
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(SwitchDefaults.IconSize),
                            tint = if (isMentorMode) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            } else {
                                if (userSettings.mentorCodeEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    },
                    colors = SwitchDefaults.colors(
                        disabledUncheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        disabledCheckedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                        disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Only show code card and access level if enabled, or if in mentor mode
                if (userSettings.mentorCodeEnabled || isMentorMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (userSettings.mentorCodeEnabled) 0.3f else 0.1f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Your Mentor Share Code", style = MaterialTheme.typography.labelMedium, color = if (userSettings.mentorCodeEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                            Text(
                                text = mentorCode ?: "Generating...",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (userSettings.mentorCodeEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { mentorCode?.let { clipboardManager.setText(AnnotatedString(it)) } },
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = userSettings.mentorCodeEnabled
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Copy")
                                }
                                if (!isMentorMode) {
                                    OutlinedButton(
                                        onClick = { settingsViewModel.regenerateMentorCode() },
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = userSettings.mentorCodeEnabled
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Regenerate")
                                    }
                                }
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Mentor Access Level",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (userSettings.mentorCodeEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                        val levels = listOf(
                            "view" to "Read Only: View your data but cannot make changes.",
                            "status" to "Status Updates: Change learning statuses (no content edits).",
                            "full" to "Edit All: Add, edit, and delete vocabulary, skills, and portfolio."
                        )
                        levels.forEach { (level, description) ->
                            val isSelected = userSettings.mentorAccessLevel == level
                            val radioEnabled = !isMentorMode && userSettings.mentorCodeEnabled
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { if (radioEnabled) settingsViewModel.setMentorAccessLevel(level) },
                                    enabled = radioEnabled
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(
                                        level.replaceFirstChar { it.uppercase() },
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (!radioEnabled && !isSelected) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (!radioEnabled) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SectionHeader(title = "Mentor Mode", icon = Icons.Default.School)
            if (isMentorMode) {
                Text(
                    "You are currently viewing another user's progress. Your actions may be limited based on their permission settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { authViewModel.exitMentorMode() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Exit Mentor Mode")
                }
            } else {
                Text(
                    "Enter a student's share code to view their progress and help them learn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                var inputCode by remember { mutableStateOf("") }
                var isValidating by remember { mutableStateOf(false) }
                var errorMessage by remember { mutableStateOf<String?>(null) }
                val scope = rememberCoroutineScope()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputCode,
                        onValueChange = {
                            if (it.length <= 5) {
                                inputCode = it.uppercase()
                                errorMessage = null
                            }
                        },
                        label = { Text("Share Code") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = errorMessage != null,
                        supportingText = errorMessage?.let { { Text(it) } }
                    )

                    Button(
                        onClick = {
                            if (inputCode.length == 5) {
                                scope.launch {
                                    isValidating = true
                                    errorMessage = null
                                    try {
                                        val ownerUid = settingsViewModel.validateCode(inputCode)
                                        if (ownerUid != null) {
                                            if (ownerUid == currentUser?.uid) {
                                                errorMessage = "Cannot mentor yourself"
                                            } else {
                                                authViewModel.enterMentorMode(ownerUid, inputCode)
                                            }
                                        } else {
                                            errorMessage = "Invalid or disabled code"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error validating code"
                                    } finally {
                                        isValidating = false
                                    }
                                }
                            }
                        },
                        enabled = inputCode.length == 5 && !isValidating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp).align(Alignment.Top)
                    ) {
                        if (isValidating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Join")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { authViewModel.signOut(context) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}
