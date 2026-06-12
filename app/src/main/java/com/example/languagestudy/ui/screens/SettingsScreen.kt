package com.example.languagestudy.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.ui.auth.AuthViewModel

@Composable
fun SettingsScreen(authViewModel: AuthViewModel = viewModel()) {
    val currentUser by authViewModel.user.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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

        // Example setting: Dark Mode (aligned with site-data.json theme property)
//        var darkModeEnabled by remember { mutableStateOf(false) }
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Text("Dark Mode", modifier = Modifier.weight(1f))
//            Switch(checked = darkModeEnabled, onCheckedChange = { darkModeEnabled = it })
//        }
    }
}
