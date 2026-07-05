package io.github.langstudy.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.langstudy.LanguageStudyApplication
import io.github.langstudy.R
import io.github.langstudy.data.local.entity.VocabEntity
import io.github.langstudy.ui.components.SoundCloudPlayer
import io.github.langstudy.ui.components.YouTubePlayer
import io.github.langstudy.ui.viewmodel.FlashcardViewModel
import io.github.langstudy.ui.viewmodel.FlashcardViewModelFactory
import io.github.langstudy.utils.UrlUtils

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
    val isSessionFinished by viewModel.isSessionFinished.collectAsState()
    val sessionStats by viewModel.sessionStats.collectAsState()

    LaunchedEffect(userId) {
        viewModel.init(userId, allVocab, categoryFilter, languageFilter)
    }

    if (isSessionFinished) {
        ReviewSessionStats(stats = sessionStats, onClose = onClose)
        return
    }

    if (reviewList.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (allVocab.isEmpty()) {
                Text(stringResource(R.string.no_vocab_found_review))
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
                title = { Text(stringResource(R.string.review_mode), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
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
                    text = stringResource(R.string.card_step_format, currentIndex + 1, reviewList.size),
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

            // Flashcard with transition to prevent "showing back of next card" bug
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentItem,
                    transitionSpec = {
                        val isNext = targetState.id != initialState.id &&
                                reviewList.indexOf(targetState) > reviewList.indexOf(initialState)

                        if (isNext) {
                            (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> -width } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> width } + fadeOut())
                        }.using(SizeTransform(clip = false))
                    },
                    label = "cardTransition"
                ) { targetItem ->
                    Flashcard(
                        vocab = targetItem,
                        isFlipped = isFlipped && targetItem.id == currentItem.id,
                        onFlip = { viewModel.flipCard() }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Status Selector
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
                    Text(stringResource(R.string.prev))
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
                    Text(if (currentIndex < reviewList.size - 1) stringResource(R.string.next) else stringResource(R.string.finish))
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
    // key(vocab.id) ensures that the rotation animation resets immediately 
    // when we switch to a new card, preventing the "briefly showing the back" bug.
    key(vocab.id) {
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
                            text = stringResource(R.string.tap_to_flip),
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
                                        stringResource(R.string.mastered),
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
                                text = stringResource(R.string.no_translation),
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
                            text = stringResource(R.string.tap_to_see_word),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
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
    val (color, labelRes) = when (status) {
        "PROFICIENT" -> Color(0xFF2E7D32) to R.string.mastered_label
        "IN_PROGRESS" -> MaterialTheme.colorScheme.primary to R.string.learning_label
        else -> MaterialTheme.colorScheme.outline to R.string.not_started_label
    }
    val label = stringResource(labelRes)

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

@Composable
fun ReviewSessionStats(
    stats: FlashcardViewModel.SessionStats,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.session_complete),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.session_reviewed_format, stats.totalReviewed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                )
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                StatRow(stringResource(R.string.mastered_label), stats.proficientCount, Color(0xFF2E7D32))
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                StatRow(stringResource(R.string.learning_label), stats.inProgressCount, MaterialTheme.colorScheme.primary)
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                StatRow(stringResource(R.string.not_started_label), stats.notStartedCount, MaterialTheme.colorScheme.outline)
            }
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            Text(stringResource(R.string.back_to_vocab), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun StatRow(label: String, count: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
