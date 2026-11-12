package com.decompress.app.ui

import android.os.Environment
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.unit.dp
import com.decompress.app.data.QuickLocation
import com.decompress.app.data.QuickLocationRepository
import com.decompress.app.data.normalizeQuickLocationPath
import com.decompress.app.data.relativePathFromInternalTreeDocumentId
import java.io.File
import com.decompress.app.ui.theme.ZippestRadius

@Composable
internal fun QuickLocationEditorDialog(
    repository: QuickLocationRepository,
    location: QuickLocation?,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var label by remember(location?.id) { mutableStateOf(location?.label.orEmpty()) }
    var path by remember(location?.id) { mutableStateOf(location?.path.orEmpty()) }
    var hasSelectedPath by remember(location?.id) {
        mutableStateOf(location != null)
    }
    var error by remember(location?.id) { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val documentId = runCatching {
            DocumentsContract.getTreeDocumentId(uri)
        }.getOrNull()
        val relativePath = documentId?.let(::relativePathFromInternalTreeDocumentId)
        if (relativePath == null) {
            error = UiStrings.get(R.string.quick_must_internal)
            return@rememberLauncherForActivityResult
        }
        val resolvedPath = resolveInternalDirectoryPath(relativePath)
        path = resolvedPath
        hasSelectedPath = true
        if (label.isBlank()) {
            label = resolvedPath.substringAfterLast('/').ifBlank { UiStrings.get(R.string.quick_title) }
        }
    }

    fun save() {
        val cleanLabel = label.trim()
        val normalizedPath = normalizeQuickLocationPath(path)
        if (cleanLabel.isEmpty()) {
            error = UiStrings.get(R.string.quick_enter_name)
            return
        }
        if (!hasSelectedPath) {
            error = UiStrings.get(R.string.quick_pick_first)
            return
        }
        if (normalizedPath == null) {
            error = UiStrings.get(R.string.quick_invalid_path)
            return
        }
        val target = File(
            Environment.getExternalStorageDirectory(),
            normalizedPath
        )
        if (!target.isDirectory) {
            error = UiStrings.get(R.string.quick_missing)
            return
        }
        if (location == null) {
            repository.add(cleanLabel, normalizedPath)
            onSaved(UiStrings.get(R.string.quick_added))
        } else {
            repository.update(location.copy(label = cleanLabel, path = normalizedPath))
            onSaved(UiStrings.get(R.string.quick_updated))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (location == null) UiStrings.get(R.string.quick_new) else UiStrings.get(R.string.quick_edit))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        error = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(UiStrings.get(R.string.name)) }
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ZippestRadius.sm),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                ) {
                    Text(
                        if (!hasSelectedPath) {
                            UiStrings.get(R.string.no_folder)
                        } else if (path.isBlank()) {
                            UiStrings.get(R.string.internal)
                        } else {
                            UiStrings.get(R.string.quick_path, path)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (error != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        error = null
                        folderPicker.launch(null)
                    }) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (hasSelectedPath) UiStrings.get(R.string.choose_again) else UiStrings.get(R.string.choose_folder),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = ::save) {
                Text(UiStrings.get(R.string.save))
            }
        },
        dismissButton = {
            TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = onDismiss) {
                Text(UiStrings.get(R.string.cancel))
            }
        }
    )
}

private fun resolveInternalDirectoryPath(relativePath: String): String {
    val normalized = normalizeQuickLocationPath(relativePath).orEmpty()
    val root = Environment.getExternalStorageDirectory()
    if (normalized.isBlank() || File(root, normalized).isDirectory) {
        return normalized
    }
    val segments = normalized.split('/').toMutableList()
    val localizedAliases = mapOf(
        UiStrings.get(R.string.downloads) to "Download",
        UiStrings.get(R.string.pictures_short) to "Pictures",
        UiStrings.get(R.string.gallery_short) to "Pictures",
        UiStrings.get(R.string.camera) to "DCIM",
        UiStrings.get(R.string.videos_short) to "Movies",
        UiStrings.get(R.string.movies) to "Movies",
        UiStrings.get(R.string.music_short) to "Music",
        UiStrings.get(R.string.documents_short) to "Documents"
    )
    segments.firstOrNull()?.let { first ->
        localizedAliases[first]?.let { segments[0] = it }
    }
    return segments.joinToString("/")
}
