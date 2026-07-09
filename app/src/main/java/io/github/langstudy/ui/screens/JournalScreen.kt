package io.github.langstudy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.langstudy.LanguageStudyApplication
import io.github.langstudy.R
import io.github.langstudy.data.local.entity.JournalEntryEntity
import io.github.langstudy.ui.components.AppButton
import io.github.langstudy.ui.components.AppFAB
import io.github.langstudy.ui.components.DeleteConfirmationDialog
import io.github.langstudy.ui.components.EmptyState
import io.github.langstudy.ui.components.GlobalSearchBar
import io.github.langstudy.ui.components.LanguageDropdown
import io.github.langstudy.ui.components.NoResultsState
import io.github.langstudy.ui.viewmodel.JournalViewModel
import io.github.langstudy.ui.viewmodel.JournalViewModelFactory
import io.github.langstudy.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    userId: String,
    sessionId: String = "",
    searchViewModel: SearchViewModel = viewModel(),
    isMentorMode: Boolean = false,
    mentorAccessLevel: String = "view"
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication
    val repository = app.journalRepository
    val viewModel: JournalViewModel = viewModel(
        key = "journal_${userId}_$sessionId",
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
        viewModel.initUserId(userId, isMentorMode)
    }

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    var title by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var mentorVisible by remember { mutableStateOf(false) }
    var mentorAccessLevelEntry by remember { mutableStateOf("view") }
    var editingEntry by remember { mutableStateOf<JournalEntryEntity?>(null) }
    var showSheet by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showSheet) {
        if (!showSheet) {
            localErrorMessage = null
            editingEntry = null
            title = ""
            contentText = ""
            language = currentLanguage
            mentorVisible = isMentorMode // Default true if mentor creates it
            mentorAccessLevelEntry = if (isMentorMode) "edit" else "view"
        }
    }

    LaunchedEffect(editingEntry) {
        editingEntry?.let {
            title = it.title
            contentText = it.content
            language = it.language
            mentorVisible = it.mentorVisible
            mentorAccessLevelEntry = it.mentorAccessLevel
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
                    contentDescription = stringResource(R.string.new_entry_cd)
                )
            }
        }
    ) { padding ->
        if (showSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        if (editingEntry == null) stringResource(R.string.new_journal_entry) else stringResource(R.string.edit_journal_entry),
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
                        label = { Text(stringResource(R.string.title_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text(stringResource(R.string.content_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 5
                    )
                    Spacer(Modifier.height(12.dp))
                    LanguageDropdown(
                        selectedLanguage = language,
                        onLanguageSelected = { language = it },
                        availableLanguages = learnedLanguages,
                        label = stringResource(R.string.language_optional),
                        includeNone = true
                    )
                    Spacer(Modifier.height(16.dp))
                    if (!isMentorMode) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SupervisorAccount,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.mentor_visible),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.enable_mentor_view_summary),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = mentorVisible,
                                onCheckedChange = { mentorVisible = it },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (mentorVisible) Icons.Rounded.Check else Icons.Rounded.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(androidx.compose.material3.SwitchDefaults.IconSize),
                                    )
                                },
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                                    checkedIconColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }

                        if (mentorVisible) {
                            Column(modifier = Modifier.padding(start = 40.dp)) {
                                Text(
                                    stringResource(R.string.mentor_access_level_entry),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = mentorAccessLevelEntry == "view",
                                        onClick = { mentorAccessLevelEntry = "view" }
                                    )
                                    Text(
                                        stringResource(R.string.view_access),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    RadioButton(
                                        selected = mentorAccessLevelEntry == "edit",
                                        onClick = { mentorAccessLevelEntry = "edit" }
                                    )
                                    Text(
                                        stringResource(R.string.edit_access),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    AppButton(
                        onClick = {
                            viewModel.saveEntry(
                                editingEntry?.id,
                                title,
                                contentText,
                                language,
                                mentorVisible,
                                mentorAccessLevelEntry,
                                editingEntry?.timestamp
                            )
                            if (title.isNotBlank() && contentText.isNotBlank()) {
                                showSheet = false
                            }
                        },
                        text = stringResource(R.string.save_entry),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GlobalSearchBar(
                query = searchQuery,
                onQueryChange = { searchViewModel.setQuery(it) },
                placeholder = stringResource(R.string.search_journal)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                if (allEntries.isEmpty()) {
                    val emptyMessage =
                        if (isMentorMode) stringResource(R.string.no_journal_mentor) else stringResource(R.string.no_journal_user)
                    EmptyState(message = emptyMessage)
                } else if (entries.isEmpty() && searchQuery.isNotEmpty()) {
                    NoResultsState(query = searchQuery)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(entries) { entry ->
                            val canEditThisEntry = if (isMentorMode) {
                                entry.mentorAccessLevel == "edit" || mentorAccessLevel == "full"
                            } else {
                                true
                            }
                            JournalItem(
                                entry = entry,
                                canEdit = canEditThisEntry,
                                isMentorMode = isMentorMode,
                                onDelete = { viewModel.deleteEntry(entry) },
                                onClick = { if (canEditThisEntry) editingEntry = entry }
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
    isMentorMode: Boolean = false,
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
            title = stringResource(R.string.delete_journal_entry_title),
            message = stringResource(R.string.delete_journal_entry_message_format, entry.title)
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
                        contentDescription = stringResource(R.string.delete),
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
                    val date =
                        java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(entry.timestamp))
                    Text(
                        date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isMentorMode && entry.mentorVisible) {
                            Icon(
                                Icons.Rounded.SupervisorAccount,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp).size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
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
}
