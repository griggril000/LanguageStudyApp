package com.example.languagestudy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PortfolioScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Portfolio", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("Title") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("YouTube or SoundCloud Link") },
                modifier = Modifier.weight(1f)
            )
            Button(onClick = {}, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterVertically)) {
                Text("Add")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(5) { index ->
                PortfolioItem(index)
            }
        }
    }
}

@Composable
fun PortfolioItem(index: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sample Project $index", style = MaterialTheme.typography.titleLarge)
            Text("https://youtube.com/watch?v=...", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedButton(onClick = {}) { Text("Edit") }
                ElevatedButton(onClick = {}) { Text("Feature") }
                TextButton(onClick = {}, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            }
        }
    }
}
