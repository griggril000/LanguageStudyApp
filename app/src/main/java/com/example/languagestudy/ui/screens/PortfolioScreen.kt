package com.example.languagestudy.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.languagestudy.data.model.PortfolioItem
import com.example.languagestudy.ui.components.SoundCloudPlayer
import com.example.languagestudy.ui.components.YouTubePlayer
import com.example.languagestudy.ui.viewmodel.PortfolioViewModel
import com.example.languagestudy.utils.UrlUtils

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var title by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.error.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.addSuccess.collect {
            title = ""
            link = ""
        }
    }

    val onPlay: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 8.dp)
            ) 
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("Link") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Button(
                        onClick = { 
                            viewModel.addItem(title, link)
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Portfolio", 
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(16.dp))
            
            if (isLoading && items.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f).fillMaxWidth()
                ) {
                    val featuredItems = items.filter { it.isTop }
                    val otherItems = items.filter { !it.isTop }
                    val canFeatureMore = featuredItems.size < 3

                    if (featuredItems.isNotEmpty()) {
                        item {
                            Text(
                                "Featured Items (${featuredItems.size}/3)", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(featuredItems, key = { it.id }) { item ->
                            FeaturedPortfolioItem(
                                item = item,
                                onDelete = { viewModel.deleteItem(item.id) },
                                onUnfeature = { viewModel.toggleFeatured(item) }
                            )
                        }
                    }

                    if (otherItems.isNotEmpty()) {
                        item {
                            Text(
                                "Other Items", 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(otherItems, key = { it.id }) { item ->
                            StandardPortfolioItem(
                                item = item,
                                onPlay = { onPlay(item.link) },
                                onDelete = { viewModel.deleteItem(item.id) },
                                onFeature = { viewModel.toggleFeatured(item) },
                                canFeature = canFeatureMore
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun FeaturedPortfolioItem(
    item: PortfolioItem,
    onDelete: () -> Unit,
    onUnfeature: () -> Unit
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
                if (item.type == "youtube" && item.videoId != null) {
                    YouTubePlayer(
                        videoId = item.videoId,
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
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onUnfeature,
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Unfeature") }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StandardPortfolioItem(
    item: PortfolioItem,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onFeature: () -> Unit,
    canFeature: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onPlay() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(item.link, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onFeature,
                    enabled = canFeature
                ) { 
                    Text(
                        "Feature",
                        color = if (canFeature) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ) 
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Rounded.Delete, 
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
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
        "https://via.placeholder.com/400x200?text=SoundCloud"
    } else {
        "https://via.placeholder.com/400x200?text=Link"
    }
}
