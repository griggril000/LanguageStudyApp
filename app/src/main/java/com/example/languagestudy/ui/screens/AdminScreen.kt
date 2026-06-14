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

    LaunchedEffect(Unit) {
        viewModel.error.collect { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }
    LaunchedEffect(Unit) {
        viewModel.message.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
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
                            onClick = { viewModel.deleteLanguage() },
                            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Language")
                        }
                    }
                }

                var newLanguageName by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newLanguageName,
                        onValueChange = { newLanguageName = it },
                        label = { Text("New Language Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    AppButton(
                        onClick = {
                            viewModel.addLanguage(newLanguageName)
                            newLanguageName = ""
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
                EditableLinkRow(
                    link = link,
                    onSave = { name, url -> viewModel.updateLink(index, name, url) },
                    onDelete = { viewModel.deleteLink(index) }
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Resource Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Resource URL") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            AppButton(
                onClick = {
                    onAdd(name, url)
                    name = ""
                    url = ""
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
    onDelete: () -> Unit
) {
    var name by remember(link) { mutableStateOf(link.name) }
    var url by remember(link) { mutableStateOf(link.url) }
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
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { isEditing = false }) { Text("Cancel") }
                    Button(
                        onClick = {
                            onSave(name, url)
                            isEditing = false
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("Save") }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(link.name, fontWeight = FontWeight.Bold)
                        Text(
                            link.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { isEditing = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}
