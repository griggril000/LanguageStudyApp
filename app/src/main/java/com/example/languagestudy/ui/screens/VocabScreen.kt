package com.example.languagestudy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.LanguageStudyApplication
import com.example.languagestudy.data.local.entity.VocabEntity
import com.example.languagestudy.ui.components.AppFAB
import com.example.languagestudy.ui.components.AppButton
import com.example.languagestudy.ui.components.DeleteConfirmationDialog
import com.example.languagestudy.ui.components.EmptyState
import com.example.languagestudy.ui.components.GlobalSearchBar
import com.example.languagestudy.ui.components.ProgressStatusLegend
import com.example.languagestudy.ui.components.StatusIcon
import com.example.languagestudy.ui.viewmodel.SearchViewModel
import com.example.languagestudy.ui.viewmodel.VocabViewModel
import com.example.languagestudy.ui.viewmodel.VocabViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabScreen(
    userId: String,
    searchViewModel: SearchViewModel = viewModel()
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication
    val viewModel: VocabViewModel = viewModel(
        key = "vocab_$userId",
        factory = VocabViewModelFactory(app.vocabRepository, app.settingsRepository)
    )
    val vocabList by viewModel.filteredVocab.collectAsState()
    val allVocab by viewModel.allVocab.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val searchQuery by searchViewModel.query.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userId) {
        viewModel.initUserId(userId)
    }

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    var word by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var language by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteCategoryConfirm by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(currentLanguage) {
        language = currentLanguage
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

    if (isAddingNewCategory) {
        AlertDialog(
            onDismissRequest = { isAddingNewCategory = false },
            title = { Text("New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            viewModel.addCategory(newCategoryName.trim())
                            category = newCategoryName.trim()
                            newCategoryName = ""
                            isAddingNewCategory = false
                        }
                    }
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNewCategory = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteCategoryConfirm && selectedCategory != null) {
        DeleteConfirmationDialog(
            onConfirm = {
                viewModel.deleteCategory(selectedCategory!!)
                showDeleteCategoryConfirm = false
            },
            onDismiss = { showDeleteCategoryConfirm = false },
            title = "Delete Category",
            message = "Are you sure you want to delete \"$selectedCategory\"? All vocabulary in this category will be deleted."
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AppFAB(
                onClick = { showAddSheet = true },
                icon = Icons.Rounded.Add,
                contentDescription = "Add Vocabulary"
            )
        }
    ) { padding ->
        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                sheetState = sheetState,
                contentWindowInsets = { WindowInsets.navigationBars }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text("Add Vocabulary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                        value = word,
                        onValueChange = { word = it },
                        label = { Text("Word") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = translation,
                        onValueChange = { translation = it },
                        label = { Text("Translation") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier.menuAnchor(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            category = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("+ New Category", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        isAddingNewCategory = true
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = language,
                            onValueChange = { language = it },
                            label = { Text("Lang") },
                            modifier = Modifier.width(80.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    AppButton(
                        onClick = {
                            viewModel.addVocab(word, translation, category, language)
                            if (word.isNotBlank()) {
                                word = ""
                                translation = ""
                                showAddSheet = false
                            }
                        },
                        text = "Add to List",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { searchViewModel.setQuery(it) },
                placeholder = "Search vocabulary..."
            )

            if (categories.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScrollableTabRow(
                        modifier = Modifier.weight(1f),
                        selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(selectedCategory) + 1,
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {}
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.setSelectedCategory(null) },
                            label = { Text("All") },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        categories.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { viewModel.setSelectedCategory(cat) },
                                label = { Text(cat) },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                    if (selectedCategory != null && selectedCategory != "General") {
                        IconButton(
                            onClick = { showDeleteCategoryConfirm = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete Category",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                if (allVocab.isNotEmpty()) {
                    Text(
                        text = "${allVocab.size} total words | ${vocabList.size} showing",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (allVocab.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        EmptyState(message = "Your vocabulary list is empty. Tap + to add words!")
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.seedSampleData() }) {
                            Text("Seed Sample Data")
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(vocabList, key = { it.id }) { vocab ->
                            VocabItem(
                                vocab = vocab, 
                                onDelete = { viewModel.deleteVocab(vocab) },
                                onEdit = { w, t, c, l -> 
                                    viewModel.updateVocab(vocab.copy(word = w, translation = t, category = c, language = l))
                                },
                                onStatusCycle = { viewModel.cycleVocabStatus(vocab) }
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
fun VocabItem(
    vocab: VocabEntity, 
    onDelete: () -> Unit,
    onEdit: (String, String, String, String) -> Unit,
    onStatusCycle: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    
    var editWord by remember { mutableStateOf(vocab.word) }
    var editTranslation by remember { mutableStateOf(vocab.translation) }
    var editCategory by remember { mutableStateOf(vocab.category) }
    var editLanguage by remember { mutableStateOf(vocab.language) }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Vocabulary") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editWord,
                        onValueChange = { editWord = it },
                        label = { Text("Word") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editTranslation,
                        onValueChange = { editTranslation = it },
                        label = { Text("Translation") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = editLanguage,
                            onValueChange = { editLanguage = it },
                            label = { Text("Lang") },
                            modifier = Modifier.width(80.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(editWord, editTranslation, editCategory, editLanguage)
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
                false // Don't dismiss yet, wait for confirmation
            } else false
        }
    )

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onConfirm = onDelete,
            onDismiss = {
                showDeleteConfirm = false
                // Reset swipe state if canceled
            },
            title = "Delete Word",
            message = "Delete \"${vocab.word}\" from your vocabulary list?"
        )
    }

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
                    .clip(RoundedCornerShape(16.dp))
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
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusIcon(status = vocab.status, onClick = onStatusCycle, size = 32.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f).clickable { showEditDialog = true }) {
                    Text(
                        vocab.word,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (vocab.status == "PROFICIENT") androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    )
                    if (vocab.translation.isNotBlank()) {
                        Text(
                            vocab.translation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            vocab.category,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    if (vocab.language.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            vocab.language,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
