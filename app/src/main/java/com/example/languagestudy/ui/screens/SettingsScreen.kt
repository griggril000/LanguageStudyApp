package com.example.languagestudy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.ui.auth.AuthViewModel
import com.example.languagestudy.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel = viewModel(),
    settingsViewModel: SettingsViewModel
) {
    val currentUser by authViewModel.user.collectAsState()
    val userSettings by settingsViewModel.userSettings.collectAsState()
    val availableLanguages by settingsViewModel.availableLanguages.collectAsState()
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        if (currentUser != null) {
            Text(
                text = "Logged in as:",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = currentUser?.email ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
        }

        Text(
            text = "Languages you are learning:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        if (availableLanguages.isEmpty()) {
            Text("Loading languages...", style = MaterialTheme.typography.bodyMedium)
        } else {
            availableLanguages.forEach { language ->
                val isSelected = userSettings.learnedLanguages.contains(language)
                val canToggle = !isSelected || userSettings.learnedLanguages.size > 1
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canToggle) { settingsViewModel.toggleLanguage(language) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { settingsViewModel.toggleLanguage(language) },
                        enabled = canToggle
                    )
                    Text(
                        text = language, 
                        modifier = Modifier.padding(start = 8.dp),
                        color = if (canToggle) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        if (userSettings.learnedLanguages.size > 1) {
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Primary Language for Study:",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Select which language to focus on in Vocab and Skills screens.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            
            userSettings.learnedLanguages.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setCurrentLanguage(language) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = userSettings.languageLearning == language,
                        onClick = { settingsViewModel.setCurrentLanguage(language) }
                    )
                    Text(text = language, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
