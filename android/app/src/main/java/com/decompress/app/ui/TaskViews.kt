package com.decompress.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.data.TaskHistoryEntry
import com.decompress.app.ui.theme.ZippestRadius

@Composable
internal fun TaskHistoryRow(
    entry: TaskHistoryEntry,
    onOpen: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(ZippestRadius.sm),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (entry.success) UiStrings.get(R.string.done) else UiStrings.get(R.string.failed),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (entry.success) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                entry.message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (entry.locationUri != null) {
                    TextButton(onClick = onOpen) { Text(UiStrings.get(R.string.open_location)) }
                }
                if (entry.retryPayload != null) {
                    TextButton(onClick = onRetry) { Text(UiStrings.get(R.string.retry)) }
                }
                TextButton(onClick = onDelete) {
                    Text(UiStrings.get(R.string.toolbar_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
internal fun TaskSectionLabel(title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
