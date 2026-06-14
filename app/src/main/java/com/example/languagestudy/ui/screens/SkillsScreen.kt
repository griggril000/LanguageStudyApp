package com.example.languagestudy.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.languagestudy.LanguageStudyApplication
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.local.entity.Subtask
import com.example.languagestudy.ui.components.AppButton
import com.example.languagestudy.ui.components.AppFAB
import com.example.languagestudy.ui.components.DeleteConfirmationDialog
import com.example.languagestudy.ui.components.EmptyState
import com.example.languagestudy.ui.components.GlobalSearchBar
import com.example.languagestudy.ui.components.LanguageDropdown
import com.example.languagestudy.ui.components.NoResultsState
import com.example.languagestudy.ui.components.ProgressStatusLegend
import com.example.languagestudy.ui.components.StatusIcon
import com.example.languagestudy.ui.viewmodel.SearchViewModel
import com.example.languagestudy.ui.viewmodel.SkillViewModel
import com.example.languagestudy.ui.viewmodel.SkillViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillsScreen(
    userId: String,
    searchViewModel: SearchViewModel = viewModel(),
    isMentorMode: Boolean = false,
    mentorAccessLevel: String = "view"
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
    val learnedLanguages by viewModel.learnedLanguages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val searchQuery by searchViewModel.query.collectAsState()
    val languageOverride by searchViewModel.selectedLanguage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val haptic = LocalHapticFeedback.current

    val canEditContent = !isMentorMode || mentorAccessLevel == "full"
    val canChangeStatus = !isMentorMode || mentorAccessLevel == "status" || mentorAccessLevel == "full"
    val isDragEnabled = searchQuery.isBlank() && selectedLanguage == null && !isMentorMode

    LaunchedEffect(languageOverride) {
        if (languageOverride != null) {
            viewModel.setSelectedLanguage(languageOverride)
        }
    }

    // Local state for drag and drop
    var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingOffset by remember { mutableStateOf(0f) }
    var initialTouchY by remember { mutableStateOf(0f) }
    var localSkillsList by remember { mutableStateOf(skillsList) }
    var autoScrollJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(skillsList) {
        if (draggedItemIndex == null) {
            localSkillsList = skillsList
        }
    }

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
            AppFAB(
                onClick = { showAddSheet = true },
                icon = Icons.Rounded.Add,
                contentDescription = "Add Skill"
            )
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
                    LanguageDropdown(
                        selectedLanguage = skillLanguage,
                        onLanguageSelected = { skillLanguage = it },
                        availableLanguages = learnedLanguages,
                        label = "Language (optional)",
                        includeNone = true
                    )
                    Spacer(Modifier.height(24.dp))
                    AppButton(
                        onClick = {
                            viewModel.addSkill(skillName, skillLanguage)
                            if (skillName.isNotBlank()) {
                                skillName = ""
                                showAddSheet = false
                            }
                        },
                        text = "Add Skills",
                        modifier = Modifier.fillMaxWidth()
                    )
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
                if (allSkills.isNotEmpty()) {
                    Text(
                        text = "${allSkills.size} total skills | ${skillsList.size} showing",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (allSkills.isEmpty()) {
                    val emptyMessage = if (isMentorMode) "This student hasn't tracked any skills yet." else "No skills tracked yet. Tap + to add one!"
                    EmptyState(message = emptyMessage)
                } else if (skillsList.isEmpty()) {
                    val currentLang = languageOverride ?: selectedLanguage
                    val message = if (searchQuery.isNotEmpty()) {
                        "No results for \"$searchQuery\""
                    } else if (currentLang != null && currentLang.isNotBlank()) {
                        if (isMentorMode) "This student hasn't tracked any skills for $currentLang yet."
                        else "You haven't tracked any skills for $currentLang yet."
                    } else {
                        "No skills found for the current filters."
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(message = message)
                    }
                } else {
                    LazyColumn(
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(isDragEnabled) {
                                if (!isDragEnabled) return@pointerInput
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        lazyListState.layoutInfo.visibleItemsInfo
                                            .firstOrNull { item ->
                                                offset.y.toInt() in item.offset..(item.offset + item.size)
                                            }?.also {
                                                draggedItemIndex = it.index
                                                initialTouchY = offset.y - it.offset
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        
                                        val currentIdx = draggedItemIndex ?: return@detectDragGesturesAfterLongPress
                                        val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIdx } ?: return@detectDragGesturesAfterLongPress
                                        
                                        val targetItemTop = change.position.y - initialTouchY
                                        draggingOffset = targetItemTop - itemInfo.offset
                                        
                                        val currentItemY = itemInfo.offset + draggingOffset
                                        val currentItemCenterY = currentItemY + itemInfo.size / 2
                                        
                                        // Check for swap
                                        val targetItem = lazyListState.layoutInfo.visibleItemsInfo.find { it ->
                                            it.index != currentIdx &&
                                            currentItemCenterY.toInt() in it.offset..(it.offset + it.size)
                                        }

                                        if (targetItem != null) {
                                            val targetIdx = targetItem.index
                                            val newList = localSkillsList.toMutableList()
                                            val item = newList.removeAt(currentIdx)
                                            newList.add(targetIdx, item)
                                            localSkillsList = newList
                                            
                                            draggedItemIndex = targetIdx
                                            // Re-calculate draggingOffset for the new position in the list
                                            draggingOffset = targetItemTop - targetItem.offset
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }

                                        // Auto-scroll logic
                                        val viewPortTop = 0
                                        val viewPortBottom = lazyListState.layoutInfo.viewportEndOffset
                                        if (currentItemY < viewPortTop + 100 && autoScrollJob == null) {
                                            autoScrollJob = coroutineScope.launch {
                                                while (true) {
                                                    lazyListState.scrollBy(-10f)
                                                    delay(10)
                                                }
                                            }
                                        } else if (currentItemY + itemInfo.size > viewPortBottom - 100 && autoScrollJob == null) {
                                            autoScrollJob = coroutineScope.launch {
                                                while (true) {
                                                    lazyListState.scrollBy(10f)
                                                    delay(10)
                                                }
                                            }
                                        } else if (currentItemY.toInt() in (viewPortTop + 100)..(viewPortBottom - 100)) {
                                            autoScrollJob?.cancel()
                                            autoScrollJob = null
                                        }
                                    },
                                    onDragEnd = {
                                        if (localSkillsList != skillsList) {
                                            viewModel.updateSkillOrder(localSkillsList)
                                        }
                                        draggedItemIndex = null
                                        draggingOffset = 0f
                                        autoScrollJob?.cancel()
                                        autoScrollJob = null
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = null
                                        draggingOffset = 0f
                                        autoScrollJob?.cancel()
                                        autoScrollJob = null
                                        localSkillsList = skillsList
                                    }
                                )
                            }
                    ) {
                        itemsIndexed(localSkillsList, key = { _, skill: SkillEntity -> skill.id }) { index: Int, skill: SkillEntity ->
                            val isDragging = index == draggedItemIndex
                            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "drag_scale")

                            SkillItem(
                                skill = skill,
                                learnedLanguages = learnedLanguages,
                                isDragEnabled = isDragEnabled,
                                canEdit = canEditContent,
                                canChangeStatus = canChangeStatus,
                                onStatusCycle = { viewModel.cycleSkillStatus(skill) },
                                onDelete = { viewModel.deleteSkill(skill) },
                                onAddSubtask = { viewModel.addSubtask(skill, it) },
                                onSubtaskStatusCycle = { viewModel.updateSubtaskStatus(skill, it) },
                                onSubtaskDelete = { viewModel.deleteSubtask(skill, it) },
                                onEditSkill = { name, lang -> viewModel.updateSkill(skill.copy(name = name, language = lang)) },
                                onEditSubtask = { subtaskId, newText -> viewModel.updateSubtaskText(skill, subtaskId, newText) },
                                modifier = (if (isDragging) Modifier else Modifier.animateItem())
                                    .graphicsLayer {
                                        translationY = if (isDragging) draggingOffset else 0f
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .zIndex(if (isDragging) 1f else 0f)
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
    learnedLanguages: List<String>,
    isDragEnabled: Boolean,
    canEdit: Boolean,
    canChangeStatus: Boolean,
    onStatusCycle: () -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onSubtaskStatusCycle: (String) -> Unit,
    onSubtaskDelete: (String) -> Unit,
    onEditSkill: (String, String) -> Unit,
    onEditSubtask: (String, String) -> Unit,
    modifier: Modifier = Modifier
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
                    LanguageDropdown(
                        selectedLanguage = editLang,
                        onLanguageSelected = { editLang = it },
                        availableLanguages = learnedLanguages,
                        label = "Language",
                        includeNone = true
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
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = canEdit,
        modifier = modifier
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
                    StatusIcon(
                        status = skill.status, 
                        onClick = if (canChangeStatus) onStatusCycle else ({}), 
                        size = 32.dp
                    )

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

                    if (canEdit) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isDragEnabled) {
                        Icon(
                            Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            if (expanded) Icons.Rounded.KeyboardArrowDown else Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (skill.progress > 0 || skill.status == "IN_PROGRESS") {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { skill.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = if (skill.progress == 100) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
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
                                canEdit = canEdit,
                                canChangeStatus = canChangeStatus,
                                onStatusCycle = { onSubtaskStatusCycle(subtask.id) },
                                onDelete = { onSubtaskDelete(subtask.id) },
                                onEdit = { onEditSubtask(subtask.id, it) }
                            )
                        }

                        if (canEdit) {
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
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
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
    canEdit: Boolean,
    canChangeStatus: Boolean,
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
        StatusIcon(
            status = subtask.status, 
            onClick = if (canChangeStatus) onStatusCycle else ({}), 
            size = 24.dp
        )
        
        Spacer(Modifier.width(8.dp))
        
        Text(
            subtask.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .then(if (canEdit) Modifier.clickable { showEditDialog = true } else Modifier),
            textDecoration = if (subtask.status == "PROFICIENT") TextDecoration.LineThrough else null
        )

        if (canEdit) {
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Delete Subtask", modifier = Modifier.size(16.dp))
            }
        }
    }
}
