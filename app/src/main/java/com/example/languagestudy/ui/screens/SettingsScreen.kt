package com.example.languagestudy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val currentUser by authViewModel.user.collectAsState()
    val userSettings by settingsViewModel.userSettings.collectAsState()
    val availableLanguages by settingsViewModel.availableLanguages.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // User Info Card
        if (currentUser != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
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
                            Icon(
                                Icons.Default.Person, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Languages Selection Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Languages you are learning", icon = Icons.Default.Language)
            
            if (availableLanguages.isEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableLanguages.forEach { language ->
                        val isSelected = userSettings.learnedLanguages.contains(language)
                        val canToggle = !isSelected || userSettings.learnedLanguages.size > 1
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { if (canToggle) settingsViewModel.toggleLanguage(language) },
                            label = { Text(language) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Primary Language Section
        if (userSettings.learnedLanguages.size > 1) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title = "Primary Language for Study", icon = Icons.Default.Done)
                Text(
                    text = "Select which language to focus on in Vocab and Skills screens.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    userSettings.learnedLanguages.forEach { language ->
                        val isSelected = userSettings.languageLearning == language
                        FilterChip(
                            selected = isSelected,
                            onClick = { settingsViewModel.setCurrentLanguage(language) },
                            label = { Text(language) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.RadioButtonChecked,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // Logout Button
        OutlinedButton(
            onClick = { authViewModel.signOut(context) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Sign Out")
        }
    }
}
