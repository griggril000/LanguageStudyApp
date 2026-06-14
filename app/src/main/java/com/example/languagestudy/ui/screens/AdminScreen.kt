package com.example.languagestudy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.languagestudy.data.model.LanguageResource
import com.example.languagestudy.ui.components.AppButton
import com.example.languagestudy.ui.components.LanguageDropdown
import com.example.languagestudy.ui.components.SectionHeader
import com.example.languagestudy.ui.viewmodel.AdminViewModel

@Composable
fun AdminScreen(viewModel: AdminViewModel) {
    val languages by viewModel.languages.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val links by viewModel.links.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    var showDeleteLanguageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.error.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.message.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    if (showDeleteLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLanguageDialog = false },
            title = { Text("Delete Language") },
            text = { Text("Are you sure you want to delete \"$selectedLanguage\" and all its resource links? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLanguage()
                        showDeleteLanguageDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Language Administration",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(title = "Manage Languages", icon = Icons.Rounded.Language)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LanguageDropdown(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { viewModel.selectLanguage(it) },
                        availableLanguages = languages,
                        modifier = Modifier.weight(1f),
                        includeNone = true
                    )

                    if (selectedLanguage.isNotBlank()) {
                        IconButton(
                            onClick = { showDeleteLanguageDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Language")
                        }
                    }
                }

                var newLanguageName by remember { mutableStateOf("") }
                var languageError by remember { mutableStateOf<String?>(null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newLanguageName,
                        onValueChange = { 
                            newLanguageName = it
                            if (it.isNotBlank()) languageError = null
                        },
                        label = { Text("New Language Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        isError = languageError != null,
                        supportingText = languageError?.let { { Text(it) } },
                        enabled = !isLoading
                    )
                    AppButton(
                        onClick = {
                            if (newLanguageName.isBlank()) {
                                languageError = "Name cannot be empty"
                            } else if (newLanguageName.contains("/")) {
                                languageError = "Invalid characters"
                            } else {
                                viewModel.addLanguage(newLanguageName)
                                newLanguageName = ""
                                languageError = null
                            }
                        },
                        text = "Add",
                        loading = isLoading
                    )
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        if (selectedLanguage.isNotBlank()) {
            item {
                SectionHeader(title = "Manage Links for $selectedLanguage", icon = Icons.Rounded.Link)
            }

            item {
                AddLinkSection(onAdd = { name, url -> viewModel.addLink(name, url) }, isLoading = isLoading)
            }

            item {
                Text(
                    "Current Links",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            itemsIndexed(links) { index, link ->
                var showDeleteDialog by remember { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Delete Link") },
                        text = { Text("Are you sure you want to delete the link \"${link.name}\"?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deleteLink(index)
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Delete") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                EditableLinkRow(
                    link = link,
                    onSave = { name, url -> viewModel.updateLink(index, name, url) },
                    onDelete = { showDeleteDialog = true },
                    isLoading = isLoading
                )
            }
            
            if (links.isEmpty()) {
                item {
                    Text(
                        "No links added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        } else {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Select a language to manage links",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddLinkSection(onAdd: (String, String) -> Unit, isLoading: Boolean) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    if (it.isNotBlank()) nameError = null
                },
                label = { Text("Resource Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it) } },
                enabled = !isLoading
            )
            OutlinedTextField(
                value = url,
                onValueChange = { 
                    url = it
                    if (it.isNotBlank()) urlError = null
                },
                label = { Text("Resource URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                isError = urlError != null,
                supportingText = urlError?.let { { Text(it) } },
                enabled = !isLoading
            )
            AppButton(
                onClick = {
                    val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url.trim() else "https://${url.trim()}"
                    val isValid = formattedUrl.contains(".") && !formattedUrl.contains(" ")
                    
                    if (name.isBlank()) {
                        nameError = "Name is required"
                    } else if (url.isBlank()) {
                        urlError = "URL is required"
                    } else if (!isValid) {
                        urlError = "Invalid URL (e.g. example.com)"
                    } else {
                        onAdd(name, url)
                        name = ""
                        url = ""
                        nameError = null
                        urlError = null
                    }
                },
                text = "Add Link",
                modifier = Modifier.fillMaxWidth(),
                loading = isLoading
            )
        }
    }
}

@Composable
fun EditableLinkRow(
    link: LanguageResource,
    onSave: (String, String) -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean
) {
    var name by remember(link) { mutableStateOf(link.name) }
    var url by remember(link) { mutableStateOf(link.url) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var urlError by remember { mutableStateOf<String?>(null) }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        if (it.isNotBlank()) nameError = null
                    },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { 
                        url = it
                        if (it.isNotBlank()) urlError = null
                    },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    isError = urlError != null,
                    supportingText = urlError?.let { { Text(it) } },
                    enabled = !isLoading
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { 
                        isEditing = false
                        name = link.name
                        url = link.url
                        nameError = null
                        urlError = null
                    }, enabled = !isLoading) { Text("Cancel") }
                    Button(
                        onClick = {
                            val formattedUrl = if (url.startsWith("http://") || url.startsWith("https://")) url.trim() else "https://${url.trim()}"
                            val isValid = formattedUrl.contains(".") && !formattedUrl.contains(" ")

                            if (name.isBlank()) {
                                nameError = "Name is required"
                            } else if (url.isBlank()) {
                                urlError = "URL is required"
                            } else if (!isValid) {
                                urlError = "Invalid URL"
                            } else {
                                onSave(name, url)
                                isEditing = false
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isLoading
                    ) { 
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(link.name.ifBlank { "No Name" }, fontWeight = FontWeight.Bold)
                        Text(
                            link.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { isEditing = true }, enabled = !isLoading) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
