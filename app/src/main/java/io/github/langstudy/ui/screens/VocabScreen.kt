package io.github.langstudy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import io.github.langstudy.data.local.entity.VocabEntity
import io.github.langstudy.navigation.NavRoute
import io.github.langstudy.ui.components.AppButton
import io.github.langstudy.ui.components.AppFAB
import io.github.langstudy.ui.components.DeleteConfirmationDialog
import io.github.langstudy.ui.components.EmptyState
import io.github.langstudy.ui.components.GlobalSearchBar
import io.github.langstudy.ui.components.LanguageDropdown
import io.github.langstudy.ui.components.LinkText
import io.github.langstudy.ui.components.ProgressStatusLegend
import io.github.langstudy.ui.components.StatusIcon
import io.github.langstudy.ui.viewmodel.SearchViewModel
import io.github.langstudy.ui.viewmodel.VocabViewModel
import io.github.langstudy.ui.viewmodel.VocabViewModelFactory
import io.github.langstudy.utils.LocalTtsManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabScreen(
    userId: String,
    onNavigate: (NavRoute) -> Unit = {},
    sessionId: String = "",
    searchViewModel: SearchViewModel = viewModel(),
    isMentorMode: Boolean = false,
    mentorAccessLevel: String = "view"
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication
    val viewModel: VocabViewModel = viewModel(
        key = "vocab_${userId}_$sessionId",
        factory = VocabViewModelFactory(app.vocabRepository, app.settingsRepository)
    )
    val vocabList by viewModel.filteredVocab.collectAsState()
    val allVocab by viewModel.allVocab.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val learnedLanguages by viewModel.learnedLanguages.collectAsState()
    val searchQuery by searchViewModel.query.collectAsState()
    val languageOverride by searchViewModel.selectedLanguage.collectAsState()

    val canEditContent = !isMentorMode || mentorAccessLevel == "full"
    val canChangeStatus =
        !isMentorMode || mentorAccessLevel == "status" || mentorAccessLevel == "full"

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

    var word by remember { mutableStateOf("") }
    var translation by remember { mutableStateOf("") }
    var exampleSentence by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var language by remember { mutableStateOf("") }
    var editingVocab by remember { mutableStateOf<VocabEntity?>(null) }
    var showEntrySheet by remember { mutableStateOf(false) }
    var isAddingNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var showDeleteCategoryConfirm by remember { mutableStateOf(false) }
    var localErrorMessage by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentLanguage) {
        if (editingVocab == null) {
            language = currentLanguage
        }
    }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            if (showEntrySheet) {
                localErrorMessage = message
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(showEntrySheet) {
        if (!showEntrySheet) {
            localErrorMessage = null
            editingVocab = null
        }
    }

    if (isAddingNewCategory) {
        AlertDialog(
            onDismissRequest = { isAddingNewCategory = false },
            title = { Text(stringResource(R.string.new_category)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category_name_label)) },
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
                ) { Text(stringResource(R.string.add)) }
            },
            dismissButton = {
                TextButton(onClick = { isAddingNewCategory = false }) { Text(stringResource(R.string.cancel)) }
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
            title = stringResource(R.string.delete_category_title),
            message = stringResource(R.string.delete_category_message_format, selectedCategory!!)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                if (canEditContent) {
                    AppFAB(
                        onClick = {
                            editingVocab = null
                            word = ""
                            translation = ""
                            exampleSentence = ""
                            category = selectedCategory ?: "General"
                            language = currentLanguage
                            showEntrySheet = true
                        },
                        icon = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_vocabulary_cd)
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchViewModel.setQuery(it) },
                    placeholder = stringResource(R.string.search_vocab)
                )

                if (categories.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScrollableTabRow(
                            modifier = Modifier.weight(1f),
                            selectedTabIndex = if (selectedCategory == null) 0 else categories.indexOf(
                                selectedCategory
                            ) + 1,
                            edgePadding = 16.dp,
                            containerColor = Color.Transparent,
                            divider = {},
                            indicator = {}
                        ) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.setSelectedCategory(null) },
                                label = { Text(stringResource(R.string.all)) },
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
                        if (allVocab.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    onNavigate(
                                        NavRoute.Flashcards(
                                            category = selectedCategory,
                                            language = currentLanguage
                                        )
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Style,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.flashcards),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (selectedCategory != null && selectedCategory != "General" && canEditContent) {
                            IconButton(
                                onClick = { showDeleteCategoryConfirm = true },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete_category_cd),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (allVocab.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.total_showing_vocab_format, allVocab.size, vocabList.size),
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
                            val emptyMessage =
                                if (isMentorMode) stringResource(R.string.no_vocab_mentor) else stringResource(R.string.no_vocab_user)
                            EmptyState(message = emptyMessage)
                            if (!isMentorMode) {
                                Spacer(Modifier.height(16.dp))
                                TextButton(onClick = { viewModel.seedSampleData() }) {
                                    Text(stringResource(R.string.seed_sample_data))
                                }
                            }
                        }
                    } else if (vocabList.isEmpty()) {
                        val currentLang = languageOverride ?: currentLanguage
                        val message = if (searchQuery.isNotEmpty()) {
                            stringResource(R.string.no_results_format, searchQuery)
                        } else if (currentLang.isNotBlank()) {
                            if (isMentorMode) stringResource(R.string.no_vocab_lang_mentor_format, currentLang)
                            else stringResource(R.string.no_vocab_lang_user_format, currentLang)
                        } else {
                            stringResource(R.string.no_vocab_filters)
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            EmptyState(message = message)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(vocabList, key = { it.id }) { vocab ->
                                VocabItem(
                                    vocab = vocab,
                                    canEdit = canEditContent,
                                    canChangeStatus = canChangeStatus,
                                    onDelete = { viewModel.deleteVocab(vocab) },
                                    onEditRequest = { v ->
                                        editingVocab = v
                                        word = v.word
                                        translation = v.translation
                                        exampleSentence = v.exampleSentence
                                        category = v.category
                                        language = v.language
                                        showEntrySheet = true
                                    },
                                    onStatusCycle = { viewModel.cycleVocabStatus(vocab) }
                                )
                            }
                        }
                        ProgressStatusLegend(
                            modifier = if (canEditContent) Modifier.padding(end = 80.dp) else Modifier
                        )
                    }
                }
            }
        }

        if (showEntrySheet) {
            ModalBottomSheet(
                onDismissRequest = { showEntrySheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp)
                ) {
                    Text(
                        if (editingVocab == null) stringResource(R.string.add_vocabulary_title)
                        else stringResource(R.string.edit_vocabulary),
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
                        value = word,
                        onValueChange = { word = it },
                        label = { Text(stringResource(R.string.word_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = translation,
                        onValueChange = { translation = it },
                        label = { Text(stringResource(R.string.translation_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exampleSentence,
                        onValueChange = { exampleSentence = it },
                        label = { Text(stringResource(R.string.example_sentence_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
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
                                text = {
                                    Text(
                                        stringResource(R.string.new_category_plus),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = {
                                    isAddingNewCategory = true
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LanguageDropdown(
                        selectedLanguage = language,
                        onLanguageSelected = { language = it },
                        availableLanguages = learnedLanguages
                    )
                    Spacer(Modifier.height(24.dp))
                    AppButton(
                        onClick = {
                            if (editingVocab == null) {
                                viewModel.addVocab(word, translation, category, language, exampleSentence)
                            } else {
                                viewModel.updateVocab(
                                    editingVocab!!.copy(
                                        word = word,
                                        translation = translation,
                                        category = category,
                                        language = language,
                                        exampleSentence = exampleSentence
                                    )
                                )
                            }
                            if (word.isNotBlank()) {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        showEntrySheet = false
                                    }
                                }
                            }
                        },
                        text = if (editingVocab == null) stringResource(R.string.add_to_list)
                               else stringResource(R.string.save_changes),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabItem(
    vocab: VocabEntity,
    canEdit: Boolean,
    canChangeStatus: Boolean,
    onDelete: () -> Unit,
    onEditRequest: (VocabEntity) -> Unit,
    onStatusCycle: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val ttsManager = LocalTtsManager.current

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
            title = stringResource(R.string.delete_word_title),
            message = stringResource(R.string.delete_word_message_format, vocab.word)
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
                StatusIcon(
                    status = vocab.status,
                    onClick = if (canChangeStatus) onStatusCycle else ({}),
                    size = 32.dp
                )
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .then(if (canEdit) Modifier.clickable {
                            onEditRequest(vocab)
                        } else Modifier)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            vocab.word,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (vocab.status == "PROFICIENT") androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        IconButton(
                            onClick = { ttsManager?.speak(vocab.word, vocab.language) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = "Speak",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (vocab.translation.isNotBlank()) {
                        LinkText(
                            vocab.translation,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                    if (vocab.exampleSentence.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                vocab.exampleSentence,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    ttsManager?.speak(
                                        vocab.exampleSentence,
                                        vocab.language
                                    )
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.VolumeUp,
                                    contentDescription = "Speak Example",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
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
