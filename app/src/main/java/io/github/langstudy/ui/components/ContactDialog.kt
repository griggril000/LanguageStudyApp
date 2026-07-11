package io.github.langstudy.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.langstudy.R

enum class ContactRequestType {
    ADD_LANGUAGE,
    TRANSLATE,
    SUGGEST_RESOURCE,
    BUG_REPORT,
    FEATURE_REQUEST,
    OTHER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDialog(
    initialType: ContactRequestType = ContactRequestType.ADD_LANGUAGE,
    initialLanguage: String = "",
    onDismiss: () -> Unit,
    onSubmit: (type: ContactRequestType, message: String, resourceName: String?, resourceLocation: String?) -> Unit
) {
    var requestType by remember { mutableStateOf(initialType) }
    var message by remember { mutableStateOf(if (initialLanguage.isNotBlank() && initialType == ContactRequestType.ADD_LANGUAGE) initialLanguage else "") }
    var resourceName by remember { mutableStateOf("") }
    var resourceLocation by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.contact_title)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.contact_info),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (requestType) {
                            ContactRequestType.ADD_LANGUAGE -> stringResource(R.string.contact_type_add_language)
                            ContactRequestType.TRANSLATE -> stringResource(R.string.contact_type_translate)
                            ContactRequestType.SUGGEST_RESOURCE -> stringResource(R.string.contact_type_suggest_resource)
                            ContactRequestType.BUG_REPORT -> stringResource(R.string.contact_type_bug_report)
                            ContactRequestType.FEATURE_REQUEST -> stringResource(R.string.contact_type_feature_request)
                            else -> stringResource(R.string.contact_type_other)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.contact_type_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        ContactRequestType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (type) {
                                            ContactRequestType.TRANSLATE -> stringResource(R.string.contact_type_translate)
                                            ContactRequestType.ADD_LANGUAGE -> stringResource(R.string.contact_type_add_language)
                                            ContactRequestType.SUGGEST_RESOURCE -> stringResource(R.string.contact_type_suggest_resource)
                                            ContactRequestType.BUG_REPORT -> stringResource(R.string.contact_type_bug_report)
                                            ContactRequestType.FEATURE_REQUEST -> stringResource(R.string.contact_type_feature_request)
                                            else -> stringResource(R.string.contact_type_other)
                                        }
                                    )
                                },
                                onClick = {
                                    requestType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (requestType == ContactRequestType.SUGGEST_RESOURCE) {
                    OutlinedTextField(
                        value = resourceName,
                        onValueChange = { resourceName = it },
                        label = { Text(stringResource(R.string.resource_name_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = resourceLocation,
                        onValueChange = { resourceLocation = it },
                        label = { Text(stringResource(R.string.resource_location_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = {
                        Text(
                            when (requestType) {
                                ContactRequestType.ADD_LANGUAGE -> stringResource(R.string.language_name_label)
                                ContactRequestType.SUGGEST_RESOURCE -> stringResource(R.string.additional_notes_label)
                                else -> stringResource(R.string.message_label)
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = if (requestType == ContactRequestType.SUGGEST_RESOURCE) 2 else 3
                )
            }
        },
        confirmButton = {
            val isEnabled = when (requestType) {
                ContactRequestType.SUGGEST_RESOURCE -> resourceName.isNotBlank() && resourceLocation.isNotBlank()
                else -> message.isNotBlank()
            }
            TextButton(
                onClick = {
                    onSubmit(
                        requestType,
                        message,
                        if (requestType == ContactRequestType.SUGGEST_RESOURCE) resourceName else null,
                        if (requestType == ContactRequestType.SUGGEST_RESOURCE) resourceLocation else null
                    )
                    onDismiss()
                },
                enabled = isEnabled
            ) {
                Text(stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
