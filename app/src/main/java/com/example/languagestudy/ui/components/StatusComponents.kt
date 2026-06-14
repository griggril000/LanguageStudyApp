package com.example.languagestudy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressStatusLegend() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Progress Status Legend:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LegendItem(Icons.Rounded.RadioButtonUnchecked, MaterialTheme.colorScheme.outline, "Not Started")
                LegendItem(Icons.Rounded.Schedule, MaterialTheme.colorScheme.primary, "In Progress")
                LegendItem(Icons.Rounded.CheckCircle, Color(0xFF2E7D32), "Proficient")
            }
        }
    }
}

@Composable
fun LegendItem(icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun StatusIcon(
    status: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 32.dp
) {
    val (icon, color) = when (status) {
        "PROFICIENT" -> Icons.Rounded.CheckCircle to Color(0xFF2E7D32) // Green
        "IN_PROGRESS" -> Icons.Rounded.Schedule to MaterialTheme.colorScheme.primary // Using theme primary
        else -> Icons.Rounded.RadioButtonUnchecked to MaterialTheme.colorScheme.outline
    }

    IconButton(onClick = onClick, modifier = modifier.size(size)) {
        Icon(icon, contentDescription = status, tint = color, modifier = Modifier.size(size * 0.75f))
    }
}
