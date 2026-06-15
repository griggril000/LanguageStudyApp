package io.github.languagestudy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
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
import io.github.languagestudy.LanguageStudyApplication
import io.github.languagestudy.data.local.entity.JournalEntryEntity
import io.github.languagestudy.ui.components.AppButton
import io.github.languagestudy.ui.components.AppFAB
import io.github.languagestudy.ui.components.DeleteConfirmationDialog
import io.github.languagestudy.ui.components.EmptyState
import io.github.languagestudy.ui.components.GlobalSearchBar
import io.github.languagestudy.ui.components.LanguageDropdown
import io.github.languagestudy.ui.components.NoResultsState
import io.github.languagestudy.ui.viewmodel.JournalViewModel
import io.github.languagestudy.ui.viewmodel.JournalViewModelFactory
import io.github.languagestudy.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    userId: String,
    searchViewModel: SearchViewModel = viewModel(),
    isMentorMode: Boolean = false,
    mentorAccessLevel: String = "view"
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication
    val repository = app.journalRepository
    val viewModel: JournalViewModel = viewModel(
        key = "journal_$userId",
        factory = JournalViewModelFactory(repository, app.settingsRepository)
    )
    val entries by viewModel.filteredEntries.collectAsState()
    val allEntries by viewModel.allEntries.collectAsState()
    val searchQuery by searchViewModel.query.collectAsState()
    val languageOverride by searchViewModel.selectedLanguage.collectAsState()
    val learnedLanguages by viewModel.learnedLanguages.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val canEditContent = !isMentorMode || mentorAccessLevel == "full"

    LaunchedEffect(languageOverride) {
        if (languageOverride != null) {
            viewModel.setCurrentLanguage(languageOverride!!)
        }
    }

    LaunchedEffect(userId) {
        viewModel.initUserId(userId)
    }

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    var title by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var editingEntry by remember { mutableStateOf<JournalEntryEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(showSheet) {
        if (!showSheet) {
            localErrorMessage = null
            editingEntry = null
            title = ""
            contentText = ""
            language = currentLanguage
        }
    }

    LaunchedEffect(editingEntry) {
        editingEntry?.let {
            title = it.title
            contentText = it.content
            language = it.language
            showSheet = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            if (showSheet) {
                localErrorMessage = message
            } else {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canEditContent) {
                AppFAB(
                    onClick = { showSheet = true },
                    icon = Icons.Rounded.EditNote,
                    contentDescription = "New Entry"
                )
            }
        }
    ) { padding ->
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState,
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        if (editingEntry == null) "New Journal Entry" else "Edit Journal Entry",
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
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 5
                    )
                    Spacer(Modifier.height(12.dp))
                    LanguageDropdown(
                        selectedLanguage = language,
                        onLanguageSelected = { language = it },
                        availableLanguages = learnedLanguages,
                        label = "Language (optional)",
                        includeNone = true
                    )
                    Spacer(Modifier.height(24.dp))
                    AppButton(
                        onClick = {
                            viewModel.saveEntry(editingEntry?.id, title, contentText, language)
                            if (title.isNotBlank() && contentText.isNotBlank()) {
                                showSheet = false
                            }
                        },
                        text = "Save Entry",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { searchViewModel.setQuery(it) },
                placeholder = "Search journal..."
            )

            Column(modifier = Modifier.padding(16.dp)) {
                if (allEntries.isEmpty()) {
                    val emptyMessage = if (isMentorMode) "This student's journal is empty." else "Your journal is empty. Tap the icon to write your first entry!"
                    EmptyState(message = emptyMessage)
                } else if (entries.isEmpty() && searchQuery.isNotEmpty()) {
                    NoResultsState(query = searchQuery)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(entries) { entry ->
                            JournalItem(
                                entry = entry,
                                canEdit = canEditContent,
                                onDelete = { viewModel.deleteEntry(entry) },
                                onClick = { if (canEditContent) editingEntry = entry }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalItem(
    entry: JournalEntryEntity,
    canEdit: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                showDeleteConfirm = true
                false
            } else false
        }
    )

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            onConfirm = onDelete,
            onDismiss = { showDeleteConfirm = false },
            title = "Delete Journal Entry",
            message = "Are you sure you want to delete \"${entry.title}\"?"
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canEdit,
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
        }
    ) {
        Card(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val date = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp))
                    Text(
                        date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (entry.language.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                entry.language,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
