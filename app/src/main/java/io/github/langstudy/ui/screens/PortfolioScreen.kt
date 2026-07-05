package io.github.langstudy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.langstudy.R
import io.github.langstudy.data.model.PortfolioItem
import io.github.langstudy.ui.components.AppButton
import io.github.langstudy.ui.components.AppFAB
import io.github.langstudy.ui.components.DeleteConfirmationDialog
import io.github.langstudy.ui.components.EmptyState
import io.github.langstudy.ui.components.GlobalSearchBar
import io.github.langstudy.ui.components.LanguageDropdown
import io.github.langstudy.ui.components.SoundCloudPlayer
import io.github.langstudy.ui.components.YouTubePlayer
import io.github.langstudy.ui.viewmodel.PortfolioViewModel
import io.github.langstudy.ui.viewmodel.SearchViewModel
import io.github.langstudy.utils.UrlUtils

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    searchViewModel: SearchViewModel = viewModel(),
    isMentorMode: Boolean = false,
    mentorAccessLevel: String = "view"
) {
    val context = LocalContext.current
    val items by viewModel.filteredItems.collectAsState()
    val allItems by viewModel.items.collectAsState()
    val searchQuery by searchViewModel.query.collectAsState()
    val languageOverride by searchViewModel.selectedLanguage.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val learnedLanguages by viewModel.learnedLanguages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val canEditContent = !isMentorMode || mentorAccessLevel == "full"
    // Portfolio doesn't have statuses, so canChangeStatus isn't really applicable here
    // But let's assume "status" allows featuring/unfeaturing
    val canChangeStatus =
        !isMentorMode || mentorAccessLevel == "status" || mentorAccessLevel == "full"

    LaunchedEffect(languageOverride) {
        if (languageOverride != null) {
            viewModel.setCurrentLanguage(languageOverride!!)
        }
    }

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    var title by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var itemLanguage by remember { mutableStateOf("") }
    var showAddSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PortfolioItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var localErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(editingItem) {
        if (editingItem != null) {
            title = editingItem!!.title
            link = editingItem!!.link
            itemLanguage = editingItem!!.language
            showAddSheet = true
        }
    }

    LaunchedEffect(showAddSheet) {
        if (!showAddSheet) {
            editingItem = null
            title = ""
            link = ""
            localErrorMessage = null
        }
    }

    LaunchedEffect(currentLanguage) {
        if (editingItem == null) {
            itemLanguage = currentLanguage
        }
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

    LaunchedEffect(Unit) {
        viewModel.addSuccess.collect {
            title = ""
            link = ""
            localErrorMessage = null
            showAddSheet = false
            editingItem = null
        }
    }

    val onPlay: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        floatingActionButton = {
            if (canEditContent) {
                AppFAB(
                    onClick = { showAddSheet = true },
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_portfolio_item_cd)
                )
            }
        }
    ) { padding ->
        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { if (!isLoading) showAddSheet = false },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        if (editingItem == null) stringResource(R.string.add_portfolio_item_title) else stringResource(R.string.edit_portfolio_item_title),
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
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text(stringResource(R.string.youtube_soundcloud_link)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    LanguageDropdown(
                        selectedLanguage = itemLanguage,
                        onLanguageSelected = { itemLanguage = it },
                        availableLanguages = learnedLanguages,
                        label = stringResource(R.string.skill_language_label)
                    )
                    Spacer(Modifier.height(24.dp))
                    AppButton(
                        onClick = {
                            if (editingItem == null) {
                                viewModel.addItem(title, link, itemLanguage)
                            } else {
                                viewModel.updateItem(editingItem!!.id, title, link, itemLanguage)
                            }
                        },
                        loading = isLoading,
                        text = if (editingItem == null) stringResource(R.string.add_to_portfolio) else stringResource(R.string.save_changes),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column {
                GlobalSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchViewModel.setQuery(it) },
                    placeholder = stringResource(R.string.search_portfolio)
                )

                if (isLoading && allItems.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (allItems.isEmpty()) {
                    val emptyMessage =
                        if (isMentorMode) stringResource(R.string.no_portfolio_mentor) else stringResource(R.string.no_portfolio_user)
                    EmptyState(message = emptyMessage)
                } else if (items.isEmpty()) {
                    val currentLang = languageOverride ?: currentLanguage
                    val message = if (searchQuery.isNotEmpty()) {
                        stringResource(R.string.no_results_format, searchQuery)
                    } else if (currentLang.isNotBlank()) {
                        if (isMentorMode) stringResource(R.string.no_portfolio_lang_mentor_format, currentLang)
                        else stringResource(R.string.no_portfolio_lang_user_format, currentLang)
                    } else {
                        stringResource(R.string.no_portfolio_filters)
                    }
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(message = message)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                    ) {
                        val featuredItems = items.filter { it.isTop }
                        val otherItems = items.filter { !it.isTop }
                        val canFeatureMore = featuredItems.size < 3

                        if (featuredItems.isNotEmpty()) {
                            stickyHeader {
                                HeaderSection(stringResource(R.string.featured_items_format, featuredItems.size))
                            }
                            items(featuredItems, key = { it.id }) { item ->
                                FeaturedPortfolioItem(
                                    item = item,
                                    canEdit = canEditContent,
                                    canChangeStatus = canChangeStatus,
                                    onEdit = { editingItem = item },
                                    onDelete = { viewModel.deleteItem(item.id) },
                                    onUnfeature = { viewModel.toggleFeatured(item) }
                                )
                            }
                        }

                        if (otherItems.isNotEmpty()) {
                            stickyHeader {
                                HeaderSection(stringResource(R.string.other_items))
                            }
                            items(otherItems, key = { it.id }) { item ->
                                StandardPortfolioItem(
                                    item = item,
                                    canEdit = canEditContent,
                                    canChangeStatus = canChangeStatus,
                                    onPlay = { onPlay(item.link) },
                                    onEdit = { editingItem = item },
                                    onDelete = { viewModel.deleteItem(item.id) },
                                    onFeature = { viewModel.toggleFeatured(item) },
                                    canFeature = canFeatureMore
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
fun HeaderSection(title: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturedPortfolioItem(
    item: PortfolioItem,
    canEdit: Boolean,
    canChangeStatus: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onUnfeature: () -> Unit
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
            title = stringResource(R.string.delete_portfolio_item_title),
            message = stringResource(R.string.delete_portfolio_item_message_format, item.title)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color.Black)
                ) {
                    val videoId = item.videoId
                    if (item.type == "youtube" && videoId != null) {
                        YouTubePlayer(
                            videoId = videoId,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (item.type == "soundcloud") {
                        SoundCloudPlayer(
                            url = item.link,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        AsyncImage(
                            model = getThumbnailUrl(item),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (canChangeStatus) {
                            OutlinedButton(
                                onClick = onUnfeature,
                                shape = RoundedCornerShape(8.dp)
                            ) { Text(stringResource(R.string.unfeature)) }
                        }

                        if (canEdit) {
                            IconButton(onClick = onEdit) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.edit),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            TextButton(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.delete))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandardPortfolioItem(
    item: PortfolioItem,
    canEdit: Boolean,
    canChangeStatus: Boolean,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFeature: () -> Unit,
    canFeature: Boolean
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
            title = stringResource(R.string.delete_portfolio_item_title),
            message = stringResource(R.string.delete_portfolio_item_message_format, item.title)
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
                    .clip(RoundedCornerShape(12.dp))
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
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onPlay() }
                ) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.link,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canChangeStatus) {
                        val featureText = stringResource(R.string.feature)
                        TextButton(
                            onClick = onFeature,
                            enabled = canFeature
                        ) {
                            Text(
                                featureText,
                                color = if (canFeature) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.delete),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getThumbnailUrl(item: PortfolioItem): String {
    val youtubeId = item.videoId ?: UrlUtils.getYouTubeId(item.link)
    return if (youtubeId != null) {
        "https://img.youtube.com/vi/$youtubeId/maxresdefault.jpg"
    } else if (UrlUtils.isSoundCloudUrl(item.link)) {
        "https://placehold.co/400x200?text=SoundCloud"
    } else {
        "https://placehold.co/400x200?text=Link"
    }
}
