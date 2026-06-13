package com.example.languagestudy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.LanguageStudyApplication
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.local.entity.Subtask
import com.example.languagestudy.ui.components.*
import com.example.languagestudy.ui.viewmodel.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    userId: String,
    searchViewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication
    val viewModel: SkillViewModel = viewModel(
        key = "skills_$userId",
        factory = SkillViewModelFactory(app.skillRepository, app.settingsRepository)
    )
    val skillsList by viewModel.filteredSkills.collectAsState()
    val allSkills by viewModel.allSkills.collectAsState()
    val availableLanguages by viewModel.availableLanguages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val searchQuery by searchViewModel.query.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.initUserId(userId)
    }

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    var skillName by remember { mutableStateOf("") }
    var skillLanguage by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(selectedLanguage) {
        skillLanguage = selectedLanguage ?: ""
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            if (showAddSheet) {
                localErrorMessage = message
            } else {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LaunchedEffect(showAddSheet) {
        if (!showAddSheet) localErrorMessage = null
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = Color(0xFFC25A1B),
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Skill")
            }
        }
    ) { padding ->
        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface,
                contentWindowInsets = { WindowInsets.navigationBars }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        "Add New Skills",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    if (localErrorMessage != null) {
                        Text(
                            text = localErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    OutlinedTextField(
                        value = skillName,
                        onValueChange = { skillName = it },
                        label = { Text("Skill Names (one per line)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = skillLanguage,
                        onValueChange = { skillLanguage = it },
                        label = { Text("Language (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.addSkill(skillName, skillLanguage)
                            if (skillName.isNotBlank()) {
                                skillName = ""
                                showAddSheet = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC25A1B))
                    ) {
                        Text("Add Skills")
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { searchViewModel.setQuery(it) },
                placeholder = "Search skills..."
            )

            if (availableLanguages.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = if (selectedLanguage == null) 0 else availableLanguages.indexOf(selectedLanguage) + 1,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = {}
                ) {
                    FilterChip(
                        selected = selectedLanguage == null,
                        onClick = { viewModel.setSelectedLanguage(null) },
                        label = { Text("All") },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    availableLanguages.forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { viewModel.setSelectedLanguage(lang) },
                            label = { Text(lang) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                if (allSkills.isEmpty()) {
                    EmptyState(message = "No skills tracked yet. Tap + to add one!")
                } else if (skillsList.isEmpty() && searchQuery.isNotEmpty()) {
                    NoResultsState(query = searchQuery)
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(skillsList, key = { it.id }) { skill ->
                            SkillItem(
                                skill = skill,
                                onStatusCycle = { viewModel.cycleSkillStatus(skill) },
                                onDelete = { viewModel.deleteSkill(skill) },
                                onAddSubtask = { viewModel.addSubtask(skill, it) },
                                onSubtaskStatusCycle = { viewModel.updateSubtaskStatus(skill, it) },
                                onSubtaskDelete = { viewModel.deleteSubtask(skill, it) },
                                onEditSkill = { name, lang -> viewModel.updateSkill(skill.copy(name = name, language = lang)) },
                                onEditSubtask = { subtaskId, newText -> viewModel.updateSubtaskText(skill, subtaskId, newText) }
                            )
                        }
                    }
                    
                    ProgressStatusLegend()
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillItem(
    skill: SkillEntity,
    onStatusCycle: () -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onSubtaskStatusCycle: (String) -> Unit,
    onSubtaskDelete: (String) -> Unit,
    onEditSkill: (String, String) -> Unit,
    onEditSubtask: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var subtaskText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(skill.name) }
    var editLang by remember { mutableStateOf(skill.language) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false },
            title = "Delete Skill",
            message = "Are you sure you want to delete the skill \"${skill.name}\"?"
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Skill") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editLang,
                        onValueChange = { editLang = it },
                        label = { Text("Language") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditSkill(editName, editLang)
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirm = true
                false
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(color),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.padding(end = 24.dp),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusIcon(status = skill.status, onClick = onStatusCycle, size = 32.dp)

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { expanded = !expanded }
                    ) {
                        Text(
                            skill.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (skill.status == "PROFICIENT") TextDecoration.LineThrough else null
                        )
                        if (skill.language.isNotBlank()) {
                            Text(
                                skill.language,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = "Edit",
                            tint = Color(0xFFC25A1B),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Icon(
                        if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                if (skill.progress > 0 || skill.status == "IN_PROGRESS") {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { skill.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = if (skill.progress == 100) Color(0xFF2E7D32) else Color(0xFFC25A1B),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        HorizontalDivider(
                            modifier = Modifier.padding(bottom = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        skill.subtasks.forEach { subtask ->
                            SubtaskItem(
                                subtask = subtask,
                                onStatusCycle = { onSubtaskStatusCycle(subtask.id) },
                                onDelete = { onSubtaskDelete(subtask.id) },
                                onEdit = { onEditSubtask(subtask.id, it) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = subtaskText,
                                onValueChange = { subtaskText = it },
                                placeholder = { Text("Add subtask...") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = {
                                if (subtaskText.isNotBlank()) {
                                    onAddSubtask(subtaskText)
                                    subtaskText = ""
                                }
                            }) {
                                Icon(
                                    Icons.Rounded.AddCircle,
                                    contentDescription = "Add",
                                    tint = Color(0xFFC25A1B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubtaskItem(
    subtask: Subtask,
    onStatusCycle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(subtask.text) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false },
            title = "Delete Subtask",
            message = "Delete subtask \"${subtask.text}\"?"
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Subtask") },
            text = {
                OutlinedTextField(value = editText, onValueChange = { editText = it }, label = { Text("Subtask") })
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(editText)
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        StatusIcon(status = subtask.status, onClick = onStatusCycle, size = 24.dp)
        
        Spacer(Modifier.width(8.dp))
        
        Text(
            subtask.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).clickable { showEditDialog = true },
            textDecoration = if (subtask.status == "PROFICIENT") TextDecoration.LineThrough else null
        )

        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Delete Subtask", modifier = Modifier.size(16.dp))
        }
    }
}

