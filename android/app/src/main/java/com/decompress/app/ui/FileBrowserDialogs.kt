@file:Suppress("LocalContextGetResourceValueCall")

package com.decompress.app.ui

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.Settings
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.decompress.app.IncomingArchive
import com.decompress.app.OpenLocationRequest
import com.decompress.app.MainActivity
import com.decompress.app.R
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.AppSettings
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.QuickLocationRepository
import kotlinx.coroutines.CoroutineScope
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.GlobalSearchHit
import com.decompress.app.data.QuickLocation
import com.decompress.app.data.QuickLocationIcon
import com.decompress.app.data.ThemeMode
import com.decompress.app.data.TrashRepository
import com.decompress.app.service.TaskNavigation
import com.decompress.app.data.TaskHistoryCodec
import com.decompress.app.data.TaskHistoryEntry
import com.decompress.app.data.TaskHistoryStore
import com.decompress.app.service.ExtractionScheduler
import com.decompress.app.service.ExtractionTaskRegistry
import com.decompress.app.service.CompressionScheduler
import com.decompress.app.service.ArchiveTaskControl
import com.decompress.app.ui.theme.ZippestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun FileBrowserDialogs(
    state: FileBrowserState,
    controller: FileBrowserController,
    settings: AppSettings,
    settingsRepository: AppSettingsRepository,
    quickLocationRepository: QuickLocationRepository,
    scope: CoroutineScope,
    renameFocus: FocusRequester,
    notify: (String) -> Unit
) {
    val context = LocalContext.current
    val selectedEntries = controller.entries.filter { it.uri in controller.selected }
    if (state.showBatchRename) {
        val previews = selectedEntries.sortedBy { it.name }
            .take(2)
            .mapIndexed { index, entry ->
                entry.name + " → " + batchNameFor(entry, index + 1, state.batchPattern)
            }
        AlertDialog(
            onDismissRequest = { state.showBatchRename = false },
            title = { Text(context.getString(R.string.dialog_batch_title, selectedEntries.size)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.batchPattern,
                        onValueChange = { state.batchPattern = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(context.getString(R.string.dialog_template)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        context.getString(R.string.dialog_preview),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    previews.forEach { line ->
                        Text(
                            line,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        context.getString(R.string.dialog_howto),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        context.getString(R.string.dialog_tokens),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        context.getString(R.string.dialog_common),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { state.batchPattern = "{name}_{001}" }) {
                            Text("{name}_{001}", maxLines = 1)
                        }
                        TextButton(onClick = { state.batchPattern = "{name}-{n}" }) {
                            Text("{name}-{n}", maxLines = 1)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { state.batchPattern = "{date}_{name}" }) {
                            Text("{date}_{name}", maxLines = 1)
                        }
                        TextButton(onClick = { state.batchPattern = "{name} ({n})" }) {
                            Text("{name} ({n})", maxLines = 1)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !state.batchRenaming && state.batchPattern.isNotBlank(),
                    onClick = {
                        state.showBatchRename = false
                        val targets = selectedEntries.sortedBy { it.name }
                        scope.launch {
                            state.batchRenaming = true
                            var renamed = 0
                            targets.forEachIndexed { index, entry ->
                                val candidate = batchNameFor(entry, index + 1, state.batchPattern)
                                if (controller.renameEntry(entry, candidate) != null) renamed++
                            }
                            state.batchRenaming = false
                            controller.clearSelection()
                            notify(context.getString(R.string.dialog_renamed_count, renamed, targets.size))
                        }
                    }
                ) {
                    Text(context.getString(R.string.dialog_rename))
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { state.showBatchRename = false }) { Text(context.getString(R.string.dialog_cancel)) }
            }
        )
    }

    if (state.repairMessage != null) {
        AlertDialog(
            onDismissRequest = { state.repairMessage = null },
            title = { Text(context.getString(R.string.dialog_repair_title)) },
            text = { Text(state.repairMessage!!) },
            confirmButton = {
                Button(onClick = { state.repairMessage = null }) { Text(context.getString(R.string.dialog_got_it)) }
            }
        )
    }

    if (state.checksumTarget != null) {
        val entry = state.checksumTarget!!
        AlertDialog(
            onDismissRequest = { state.checksumTarget = null },
            title = { Text(context.getString(R.string.dialog_checksum)) },
            text = {
                Column {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(12.dp))
                    if (state.checksumBusy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(context.getString(R.string.dialog_calculating), style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (state.checksumResult == null) {
                        Text(context.getString(R.string.dialog_calc_failed), style = MaterialTheme.typography.labelMedium)
                    } else {
                        val (sha, md5) = state.checksumResult!!
                        Text(context.getString(R.string.dialog_sha256_stronger), style = MaterialTheme.typography.labelSmall)
                        Text(
                            sha,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(
                                android.content.ClipboardManager::class.java
                            )
                            clipboard?.setPrimaryClip(ClipData.newPlainText("SHA-256", sha))
                            notify(context.getString(R.string.dialog_sha_copied))
                        }) {
                            Text(context.getString(R.string.dialog_copy_sha))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(context.getString(R.string.dialog_md5_faster), style = MaterialTheme.typography.labelSmall)
                        Text(
                            md5,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(
                                android.content.ClipboardManager::class.java
                            )
                            clipboard?.setPrimaryClip(ClipData.newPlainText("MD5", md5))
                            notify(context.getString(R.string.dialog_md5_copied))
                        }) {
                            Text(context.getString(R.string.dialog_copy_md5))
                        }
                        Text(
                            context.getString(R.string.dialog_checksum_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { state.checksumTarget = null }) { Text(context.getString(R.string.dialog_close)) }
            }
        )
    }

    if (state.showDeleteConfirmation) {
        val itemCount = selectedEntries.size
        DeleteConfirmationDialog(
            itemCount = itemCount,
            useTrash = settings.useTrash,
            onDismiss = { state.showDeleteConfirmation = false },
            onConfirm = {
                state.showDeleteConfirmation = false
                val targets = selectedEntries.toList()
                if (targets.isNotEmpty()) {
                    controller.beginDelete(targets)
                    scope.launch {
                        val count = controller.commitDelete(targets)
                        notify(
                            when {
                                count == targets.size && settings.useTrash ->
                                    context.getString(R.string.dialog_moved_to_trash, count)
                                count == targets.size ->
                                    context.getString(R.string.dialog_deleted_permanently, count)
                                count <= 0 ->
                                    context.getString(R.string.dialog_delete_failed_restored)
                                else ->
                                    context.getString(R.string.dialog_deleted_partial, count, targets.size - count)
                            }
                        )
                    }
                }
            }
        )
    }

    if (state.showTrash) {
        TrashSheet(
            onDismiss = {
                state.showTrash = false
                scope.launch { controller.refresh() }
            },
            onChanged = { message ->
                scope.launch {
                    controller.refresh()
                    notify(message)
                }
            }
        )
    }

    if (state.showQuickLocationEditor) {
        QuickLocationEditorDialog(
            repository = quickLocationRepository,
            location = null,
            onDismiss = { state.showQuickLocationEditor = false },
            onSaved = { message ->
                state.showQuickLocationEditor = false
                notify(message)
            }
        )
    }

    if (state.showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { state.showNewFolderDialog = false },
            title = { Text(context.getString(R.string.dialog_new_folder)) },
            text = {
                OutlinedTextField(
                    value = state.newFolderName,
                    onValueChange = { state.newFolderName = it },
                    singleLine = true,
                    label = { Text(context.getString(R.string.dialog_name)) }
                )
            },
            confirmButton = {
                Button(
                    enabled = state.newFolderName.isNotBlank(),
                    onClick = {
                        scope.launch {
                            val created = controller.createFolder(state.newFolderName.trim())
                            state.showNewFolderDialog = false
                            notify(if (created) context.getString(R.string.dialog_folder_created) else context.getString(R.string.dialog_create_failed))
                        }
                    }
                ) {
                    Text(context.getString(R.string.dialog_create))
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { state.showNewFolderDialog = false }) {
                    Text(context.getString(R.string.dialog_cancel))
                }
            }
        )
    }

    if (state.showRenameDialog) {
        AlertDialog(
            onDismissRequest = { state.showRenameDialog = false },
            title = { Text(context.getString(R.string.dialog_rename)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = state.renameValue,
                        onValueChange = { state.renameValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(renameFocus),
                        singleLine = true,
                        label = { Text(context.getString(R.string.dialog_new_name)) },
                        trailingIcon = {
                            if (state.renameValue.text.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        state.renameValue = clearNameField(state.renameValue)
                                    }
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = context.getString(R.string.dialog_clear_name)
                                    )
                                }
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        context.getString(R.string.dialog_suffix),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(".7z", ".zip", ".rar").forEach { suffix ->
                            FilterChip(
                                selected = selectedFileSuffix(state.renameValue)
                                    .equals(suffix, ignoreCase = true),
                                onClick = {
                                    state.renameValue = replaceSelectedFileSuffix(
                                        state.renameValue,
                                        suffix
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text(suffix.uppercase(), maxLines = 1) }
                            )
                        }
                        FilterChip(
                            selected = selectedFileSuffix(state.renameValue).lowercase() !in
                                setOf(".7z", ".zip", ".rar"),
                            onClick = {
                                state.customSuffix = selectedFileSuffix(state.renameValue)
                                state.showCustomSuffixDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text(context.getString(R.string.dialog_custom), maxLines = 1) }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = state.renameValue.text.isNotBlank(),
                    onClick = {
                        val entry = selectedEntries.singleOrNull()
                        val name = state.renameValue.text.trim()
                        state.showRenameDialog = false
                        if (entry == null || !controller.beginRename(entry, name)) {
                            notify(context.getString(R.string.dialog_rename_failed_exists))
                            return@Button
                        }
                        scope.launch {
                            val renamed = controller.commitRename(entry, name)
                            if (renamed == null) {
                                notify(context.getString(R.string.dialog_rename_failed_restored))
                            }
                        }
                    }
                ) {
                    Text(context.getString(R.string.dialog_save))
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { state.showRenameDialog = false }) {
                    Text(context.getString(R.string.dialog_cancel))
                }
            }
        )
    }

    if (state.showCustomSuffixDialog) {
        AlertDialog(
            onDismissRequest = { state.showCustomSuffixDialog = false },
            title = { Text(context.getString(R.string.dialog_custom_suffix)) },
            text = {
                OutlinedTextField(
                    value = state.customSuffix,
                    onValueChange = { state.customSuffix = it },
                    singleLine = true,
                    label = { Text(context.getString(R.string.dialog_suffix)) },
                    supportingText = { Text(context.getString(R.string.dialog_suffix_hint)) }
                )
            },
            confirmButton = {
                Button(
                    enabled = sanitizeFileSuffix(state.customSuffix).isNotEmpty(),
                    onClick = {
                        state.renameValue = replaceSelectedFileSuffix(
                            state.renameValue,
                            sanitizeFileSuffix(state.customSuffix)
                        )
                        state.showCustomSuffixDialog = false
                    }
                ) {
                    Text(context.getString(R.string.dialog_apply))
                }
            },
            dismissButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),onClick = { state.showCustomSuffixDialog = false }) {
                    Text(context.getString(R.string.dialog_cancel))
                }
            }
        )
    }

    state.openWithRequest?.let { request ->
        OpenWithDialog(
            request = request,
            onDismiss = { state.openWithRequest = null },
            onLaunch = { option, rememberChoice ->
                val opened = launchOpenWith(
                    context,
                    request,
                    option,
                    rememberChoice
                )
                state.openWithRequest = null
                if (!opened) notify(context.getString(R.string.dialog_cannot_open))
            }
        )
    }

    if (state.showTaskQueue) {
        TaskQueueDialog(onDismiss = { state.showTaskQueue = false })
    }

    state.compressTarget?.let { target ->
        CompressionSheet(
            target = target,
            onDismiss = { state.compressTarget = null },
            onStarted = {
                state.compressTarget = null
                controller.clearSelection()
                notify(context.getString(R.string.dialog_queued_background))
            }
        )
    }
}
