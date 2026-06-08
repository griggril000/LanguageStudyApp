package com.example.languagestudy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import coil3.compose.AsyncImage
import com.example.languagestudy.data.model.PortfolioItem
import com.example.languagestudy.ui.viewmodel.PortfolioViewModel

@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = viewModel()) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var title by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Portfolio", 
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(), 
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("YouTube or SoundCloud Link") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = { 
                    if (title.isNotBlank() && link.isNotBlank()) {
                        viewModel.addItem(title, link)
                        title = ""
                        link = ""
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add")
            }
        }
        
        Spacer(Modifier.height(24.dp))
        
        if (isLoading && items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val featuredItems = items.filter { it.isFeatured }
                val otherItems = items.filter { !it.isFeatured }

                if (featuredItems.isNotEmpty()) {
                    item {
                        Text(
                            "Featured Items", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(featuredItems) { item ->
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
                    items(otherItems) { item ->
                        StandardPortfolioItem(
                            item = item,
                            onDelete = { viewModel.deleteItem(item.id) },
                            onFeature = { viewModel.toggleFeatured(item) }
                        )
                    }
                }
            }
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
                    .height(200.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = getThumbnailUrl(item.link),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Play button overlay placeholder
                Surface(
                    modifier = Modifier.align(Alignment.Center).size(64.dp),
                    shape = RoundedCornerShape(50),
                    color = Color.Red.copy(alpha = 0.9f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.PlayArrow, 
                            contentDescription = "Play", 
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) { Text("Edit") }
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
    onDelete: () -> Unit,
    onFeature: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                TextButton(onClick = {}) { Text("Edit") }
                TextButton(onClick = onFeature) { Text("Feature") }
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

private fun getThumbnailUrl(link: String): String {
    // Simple logic to extract YouTube thumbnail
    return if (link.contains("youtube.com") || link.contains("youtu.be")) {
        val id = link.substringAfterLast("/").substringAfter("v=")
        "https://img.youtube.com/vi/$id/maxresdefault.jpg"
    } else {
        "https://via.placeholder.com/400x200"
    }
}
