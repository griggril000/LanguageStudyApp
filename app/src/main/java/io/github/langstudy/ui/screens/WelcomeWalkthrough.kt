package io.github.langstudy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.langstudy.R
import io.github.langstudy.ui.theme.SuccessGreen
import io.github.langstudy.ui.viewmodel.SettingsViewModel

@Composable
fun WelcomeWalkthrough(
    viewModel: SettingsViewModel,
    email: String? = null,
    onDismiss: () -> Unit,
    onFinish: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    val totalSteps = 6

    val uriHandler = LocalUriHandler.current
    val availableLanguages by viewModel.availableLanguages.collectAsState(initial = emptyList())
    var selectedLang by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (currentStep) {
                                1 -> stringResource(R.string.welcome_title_1)
                                2 -> stringResource(R.string.welcome_title_2)
                                3 -> stringResource(R.string.welcome_title_3)
                                4 -> stringResource(R.string.welcome_title_4)
                                5 -> stringResource(R.string.welcome_title_5)
                                6 -> stringResource(R.string.welcome_title_6)
                                else -> ""
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.welcome_step_format, currentStep, totalSteps),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Content
                Box(
                    modifier = Modifier
                        .heightIn(min = 160.dp)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        1 -> WelcomeStep()
                        2 -> SelectLanguageStep(
                            selectedLang = selectedLang,
                            onLangSelected = {
                                selectedLang = it
                                showError = false
                            },
                            availableLanguages = availableLanguages,
                            onRequestLanguage = { langName ->
                                viewModel.submitLanguageRequest(langName, "Requested during onboarding.", email)
                            }
                        )

                        3 -> VocabStep()
                        4 -> SkillsStep()
                        5 -> PortfolioStep()
                        6 -> FinalStep()
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep in 3..5) {
                        TextButton(
                            onClick = { currentStep = totalSteps },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                stringResource(R.string.skip),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val isLastStep = currentStep == totalSteps
                    Button(
                        onClick = {
                            if (currentStep == 2 && selectedLang.isEmpty()) {
                                showError = true
                            } else if (currentStep < totalSteps) {
                                if (currentStep == 2) {
                                    viewModel.setCurrentLanguage(selectedLang)
                                }
                                currentStep++
                            } else {
                                onFinish()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLastStep) SuccessGreen else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLastStep) stringResource(R.string.welcome_finish_start) else stringResource(R.string.next),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (!isLastStep) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showError) {
            AlertDialog(
                onDismissRequest = { showError = false },
                text = { Text(stringResource(R.string.welcome_select_lang_error)) },
                confirmButton = {
                    TextButton(onClick = { showError = false }) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Text(
        text = stringResource(R.string.welcome_body_1),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectLanguageStep(
    selectedLang: String,
    onLangSelected: (String) -> Unit,
    availableLanguages: List<String>,
    onRequestLanguage: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.welcome_select_lang_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(16.dp))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLang.ifBlank { stringResource(R.string.welcome_select_lang_placeholder) },
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                availableLanguages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang) },
                        onClick = {
                            onLangSelected(lang)
                            expanded = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.welcome_lang_not_listed),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    onClick = {
                        expanded = false
                        onLangSelected("Unlisted")
                    }
                )
            }
        }

        if (selectedLang == "Unlisted") {
            var requestedLang by remember { mutableStateOf("") }
            var requestSent by remember { mutableStateOf(false) }

            Spacer(modifier = Modifier.height(16.dp))
            if (!requestSent) {
                OutlinedTextField(
                    value = requestedLang,
                    onValueChange = { requestedLang = it },
                    label = { Text(stringResource(R.string.welcome_lang_request_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (requestedLang.isNotBlank()) {
                                    onRequestLanguage(requestedLang)
                                    requestSent = true
                                }
                            },
                            enabled = requestedLang.isNotBlank()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.Send,
                                contentDescription = stringResource(R.string.welcome_lang_request_submit)
                            )
                        }
                    }
                )
                Text(
                    text = stringResource(R.string.welcome_lang_request_soon),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                )
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.welcome_lang_request_sent_format, requestedLang),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.welcome_lang_switch_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.welcome_lang_resource_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.welcome_lang_request_settings_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VocabStep() {
    Column {
        Text(
            text = stringResource(R.string.welcome_vocab_info),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SkillsStep() {
    Column {
        Text(
            text = stringResource(R.string.welcome_skills_info),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PortfolioStep() {
    Column {
        Text(
            text = stringResource(R.string.welcome_portfolio_info),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FinalStep() {
    Column {
        Text(
            text = stringResource(R.string.welcome_final_info),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
