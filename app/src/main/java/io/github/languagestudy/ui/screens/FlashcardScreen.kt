package io.github.languagestudy.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.languagestudy.LanguageStudyApplication
import io.github.languagestudy.data.local.entity.VocabEntity
import io.github.languagestudy.ui.components.SoundCloudPlayer
import io.github.languagestudy.ui.components.YouTubePlayer
import io.github.languagestudy.ui.viewmodel.FlashcardViewModel
import io.github.languagestudy.ui.viewmodel.FlashcardViewModelFactory
import io.github.languagestudy.utils.UrlUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardScreen(
    userId: String,
    allVocab: List<VocabEntity>,
    categoryFilter: String? = null,
    languageFilter: String? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as LanguageStudyApplication
    val viewModel: FlashcardViewModel = viewModel(
        factory = FlashcardViewModelFactory(app.vocabRepository)
    )

    val reviewList by viewModel.reviewList.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val isFlipped by viewModel.isFlipped.collectAsState()

    LaunchedEffect(userId) {
        viewModel.init(userId, allVocab, categoryFilter, languageFilter)
    }

    if (reviewList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (allVocab.isEmpty()) {
                Text("No vocabulary found to review.")
            } else {
                CircularProgressIndicator()
            }
        }
        return
    }

    val currentItem = reviewList.getOrNull(currentIndex) ?: return

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Review Mode", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            val progress = (currentIndex + 1).toFloat() / reviewList.size
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Card ${currentIndex + 1} of ${reviewList.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(24.dp))

            // Flashcard
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Flashcard(
                    vocab = currentItem,
                    isFlipped = isFlipped,
                    onFlip = { viewModel.flipCard() }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Status Selector (1, 2, 3 shortcuts in JS)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusButton(
                        status = "NOT_STARTED",
                        isActive = currentItem.status == "NOT_STARTED",
                        onClick = { viewModel.updateStatus("NOT_STARTED") }
                    )
                    StatusButton(
                        status = "IN_PROGRESS",
                        isActive = currentItem.status == "IN_PROGRESS",
                        onClick = { viewModel.updateStatus("IN_PROGRESS") }
                    )
                    StatusButton(
                        status = "PROFICIENT",
                        isActive = currentItem.status == "PROFICIENT",
                        onClick = { viewModel.updateStatus("PROFICIENT") }
                    )
                }
            }

            // Navigation Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { viewModel.prevCard() },
                    enabled = currentIndex > 0,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Prev")
                }

                Button(
                    onClick = {
                        if (currentIndex < reviewList.size - 1) {
                            viewModel.nextCard()
                        } else {
                            onClose()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(if (currentIndex < reviewList.size - 1) "Next" else "Finish")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (currentIndex < reviewList.size - 1) Icons.AutoMirrored.Rounded.ArrowForward else Icons.Rounded.Check,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
fun Flashcard(
    vocab: VocabEntity,
    isFlipped: Boolean,
    onFlip: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "cardRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Front Side
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = vocab.word,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Tap to flip",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Back Side
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .graphicsLayer { rotationY = 180f },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (vocab.status == "PROFICIENT") {
                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "MASTERED",
                                    color = Color(0xFF2E7D32),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val youtubeId = UrlUtils.getYouTubeId(vocab.translation)
                    val isSoundCloud = UrlUtils.isSoundCloudUrl(vocab.translation)

                    val cleanTranslation = vocab.translation
                        .replace(Regex("https?://\\S+"), "")
                        .trim()

                    if (cleanTranslation.isNotBlank()) {
                        Text(
                            text = cleanTranslation,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else if (youtubeId == null && !isSoundCloud) {
                        Text(
                            text = "(no translation)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }

                    if (youtubeId != null || isSoundCloud) {
                        Spacer(Modifier.height(24.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .graphicsLayer {
                                    // Ensure web content doesn't flip again
                                }
                        ) {
                            if (youtubeId != null) {
                                YouTubePlayer(videoId = youtubeId)
                            } else if (isSoundCloud) {
                                SoundCloudPlayer(url = vocab.translation)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Tap to see word",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatusButton(
    status: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val (color, label) = when (status) {
        "PROFICIENT" -> Color(0xFF2E7D32) to "Mastered"
        "IN_PROGRESS" -> MaterialTheme.colorScheme.primary to "Learning"
        else -> MaterialTheme.colorScheme.outline to "Not Started"
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = if (isActive) color else Color.Transparent,
            border = if (isActive) null else androidx.compose.foundation.BorderStroke(
                1.dp,
                color.copy(alpha = 0.5f)
            ),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                val icon = when (status) {
                    "PROFICIENT" -> Icons.Rounded.CheckCircle
                    "IN_PROGRESS" -> Icons.Rounded.Schedule
                    else -> Icons.Rounded.RadioButtonUnchecked
                }
                Icon(
                    icon,
                    contentDescription = status,
                    tint = if (isActive) Color.White else color,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
