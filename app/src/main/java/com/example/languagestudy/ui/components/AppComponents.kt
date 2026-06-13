package com.example.languagestudy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    loading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(text)
        }
    }
}

@Composable
fun AppFAB(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
