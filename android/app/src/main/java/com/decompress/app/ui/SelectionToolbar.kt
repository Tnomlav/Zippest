@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.decompress.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun SelectionToolbar(
    onExtract: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SelectionAction(Icons.Outlined.Archive, UiStrings.get(R.string.toolbar_extract), onExtract)
            SelectionAction(Icons.Outlined.ContentCopy, UiStrings.get(R.string.toolbar_copy), onCopy)
            SelectionAction(Icons.Outlined.DriveFileMove, UiStrings.get(R.string.toolbar_move), onMove)
            SelectionAction(Icons.Outlined.Edit, UiStrings.get(R.string.toolbar_rename), onRename)
            SelectionAction(Icons.Outlined.Delete, UiStrings.get(R.string.toolbar_delete), onDelete)
            SelectionMenu(
                expanded = menuExpanded,
                onClick = { onMenuExpandedChange(true) }
            )
        }
    }
}

@Composable
private fun RowScope.SelectionMenu(
    expanded: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.weight(1f),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = UiStrings.get(R.string.toolbar_menu),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                UiStrings.get(R.string.toolbar_menu),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RowScope.SelectionAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .combinedClickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
