@file:Suppress("LocalContextGetResourceValueCall")

@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.decompress.app.ui

import android.content.Context
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.R
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.NameText

@Composable
internal fun EntryRow(
    entry: BrowserEntry,
    selectionMode: Boolean,
    selected: Boolean,
    showThumbnail: Boolean,
    showArchivePreview: Boolean,
    onClick: () -> Unit,
    onPreviewArchive: () -> Unit,
    onExtractFile: (() -> Unit)?,
    onLongClick: () -> Unit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .semantics(mergeDescendants = true) {
                contentDescription = "${entry.name}，${entryMeta(context, entry)}"
                if (selectionMode) {
                    stateDescription = if (selected) context.getString(R.string.browser_list_selected) else context.getString(R.string.browser_list_unselected)
                }
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Checkbox(
                checked = selected,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.size(48.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        EntryLeading(entry, showThumbnail)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = if (NameText.hasRtlScript(entry.name)) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = entryMeta(context, entry),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (entry.isDirectory) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (entry.isArchive && showArchivePreview) {
            IconButton(
                onClick = onPreviewArchive,
                enabled = !selectionMode
            ) {
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = context.getString(R.string.browser_preview_archive),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (onExtractFile != null) {
            IconButton(onClick = onExtractFile) {
                Icon(
                    Icons.Outlined.Archive,
                    contentDescription = context.getString(R.string.browser_extract_named, entry.name),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

internal fun entryMeta(context: Context, entry: BrowserEntry): String {
    val date = if (entry.lastModified > 0L) {
        DateUtils.getRelativeTimeSpanString(
            entry.lastModified,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()
    } else {
        ""
    }
    return if (entry.isDirectory) {
        listOf(context.getString(R.string.browser_folder), date).filter { it.isNotBlank() }.joinToString(" · ")
    } else {
        listOf(
            Formatter.formatShortFileSize(context, entry.size),
            date
        ).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
