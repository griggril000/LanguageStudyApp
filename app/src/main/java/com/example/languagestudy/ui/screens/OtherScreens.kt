package com.example.languagestudy.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun VocabScreen() {
    PlaceholderScreen("Vocab Screen")
}

@Composable
fun SkillsScreen() {
    PlaceholderScreen("Skills Screen")
}

@Composable
fun JournalScreen() {
    PlaceholderScreen("Journal Screen")
}

@Composable
fun AdminScreen() {
    PlaceholderScreen("Admin Screen")
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineLarge)
    }
}
