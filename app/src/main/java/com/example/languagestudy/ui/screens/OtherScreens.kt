package com.example.languagestudy.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.LanguageStudyApplication
import com.example.languagestudy.data.local.entity.JournalEntryEntity
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.local.entity.VocabEntity
import com.example.languagestudy.ui.auth.AuthViewModel
import com.example.languagestudy.ui.components.GlobalSearchBar
import com.example.languagestudy.ui.viewmodel.*

@Composable
fun VocabScreen(userId: String) {
    val context = LocalContext.current
    val repository = (context.applicationContext as LanguageStudyApplication).vocabRepository
    val viewModel: VocabViewModel = viewModel(
        key = "vocab_$userId",
        factory = VocabViewModelFactory(repository)
    )
    val vocabList by viewModel.filteredVocab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var word by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search vocabulary..."
            )

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = word,
                        onValueChange = { word = it },
                        label = { Text("Word") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = translation,
                        onValueChange = { translation = it },
                        label = { Text("Translation") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        viewModel.addVocab(word, translation, "General", "en")
                        if (word.isNotBlank() && translation.isNotBlank()) {
                            word = ""
                            translation = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(vocabList) { vocab ->
                        VocabItem(vocab, onDelete = { viewModel.deleteVocab(vocab) })
                    }
                }
            }
        }
    }
}

@Composable
fun VocabItem(vocab: VocabEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vocab.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(vocab.translation, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
fun SkillsScreen(userId: String) {
    val context = LocalContext.current
    val repository = (context.applicationContext as LanguageStudyApplication).skillRepository
    val viewModel: SkillViewModel = viewModel(
        key = "skills_$userId",
        factory = SkillViewModelFactory(repository)
    )
    val skillsList by viewModel.filteredSkills.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var skillName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search skills..."
            )

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        label = { Text("New Skill") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    IconButton(onClick = {
                        viewModel.addSkill(skillName, "Beginner")
                        if (skillName.isNotBlank()) {
                            skillName = ""
                        }
                    }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add")
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(skillsList) { skill ->
                        SkillItem(skill, onProgressChange = { viewModel.updateProgress(skill, it) })
                    }
                }
            }
        }
    }
}

@Composable
fun SkillItem(skill: SkillEntity, onProgressChange: (Int) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(skill.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Level: ${skill.level} | Status: ${skill.status}", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = skill.progress.toFloat(),
                onValueChange = { onProgressChange(it.toInt()) },
                valueRange = 0f..100f
            )
            Text("Progress: ${skill.progress}%", style = MaterialTheme.typography.bodySmall)

            if (skill.subtasks.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Subtasks:", style = MaterialTheme.typography.labelSmall)
                skill.subtasks.forEach { subtask ->
                    Text("• ${subtask.text} [${subtask.status}]", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun JournalScreen(userId: String) {
    val context = LocalContext.current
    val repository = (context.applicationContext as LanguageStudyApplication).journalRepository
    val viewModel: JournalViewModel = viewModel(
        key = "journal_$userId",
        factory = JournalViewModelFactory(repository)
    )
    val entries by viewModel.filteredEntries.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search journal..."
            )

            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = contentText,
                    onValueChange = { contentText = it },
                    label = { Text("Content") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
                Button(
                    onClick = {
                        viewModel.addEntry(title, contentText)
                        if (title.isNotBlank() && contentText.isNotBlank()) {
                            title = ""
                            contentText = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Save Entry")
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries) { entry ->
                        JournalItem(entry, onDelete = { viewModel.deleteEntry(entry) })
                    }
                }
            }
        }
    }
}

@Composable
fun JournalItem(entry: JournalEntryEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                }
            }
            Text(entry.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun AdminScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Admin Screen", style = MaterialTheme.typography.headlineLarge)
    }
}

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
