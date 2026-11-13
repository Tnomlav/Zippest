@file:Suppress("LocalContextGetResourceValueCall")

package com.decompress.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.decompress.app.R
import com.decompress.app.data.AppSettings
import com.decompress.app.data.AppSettingsRepository
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.FileBrowserController
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FileBrowserMenus(
    state: FileBrowserState,
    controller: FileBrowserController,
    settings: AppSettings,
    settingsRepository: AppSettingsRepository,
    padding: PaddingValues,
    scope: CoroutineScope,
    onOpenSettings: () -> Unit,
    onCompressSelected: () -> Unit,
    onOpenWithFile: (BrowserEntry) -> Unit,
    onOpenArchive: (BrowserEntry) -> Unit,
    onNotify: (String) -> Unit,
    onClosePanels: () -> Unit,
    onDismissDialogs: () -> Unit
) {
    val context = LocalContext.current
    val selectedEntries = controller.entries.filter { it.uri in controller.selected }

    if (state.sortExpanded) {
        ZippestBarAttachedMenu(
            alignment = Alignment.TopEnd,
            panelModifier = Modifier.padding(
                top = padding.calculateTopPadding(),
                end = 4.dp
            ),
            onDismissRequest = { state.sortExpanded = false }
        ) {
            BrowserSortMode.entries.forEach { mode ->
                val active = settings.defaultSortMode == mode
                ZippestMenuItem(
                    text = { Text(mode.label()) },
                    onClick = {
                        settingsRepository.update {
                            if (it.defaultSortMode == mode) {
                                it.copy(sortAscending = !it.sortAscending)
                            } else {
                                it.copy(
                                    defaultSortMode = mode,
                                    sortAscending = mode == BrowserSortMode.NAME ||
                                        mode == BrowserSortMode.TYPE
                                )
                            }
                        }
                        state.sortExpanded = false
                    },
                    selected = active,
                    trailingIcon = {
                        if (active) {
                            Text(
                                if (settings.sortAscending) "↑" else "↓",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                )
            }
        }
    }
    if (state.moreExpanded) {
        ZippestBarAttachedMenu(
            alignment = Alignment.TopEnd,
            panelModifier = Modifier.padding(
                top = padding.calculateTopPadding(),
                end = 4.dp
            ),
            onDismissRequest = { state.moreExpanded = false }
        ) {
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_sort)) },
                leadingIcon = { Icon(Icons.Outlined.Sort, null) },
                onClick = {
                    state.moreExpanded = false
                    state.sortExpanded = true
                }
            )
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_trash)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = {
                    state.moreExpanded = false
                    onClosePanels()
                    onDismissDialogs()
                    controller.clearSelection()
                    state.showTrash = true
                }
            )
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_tasks)) },
                leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
                onClick = {
                    state.moreExpanded = false
                    onClosePanels()
                    onDismissDialogs()
                    state.showTaskQueue = true
                }
            )
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_settings)) },
                leadingIcon = { Icon(Icons.Outlined.Tune, null) },
                onClick = {
                    state.moreExpanded = false
                    onOpenSettings()
                }
            )
        }
    }
    if (state.selectionMenuExpanded) {
        ZippestBarAttachedMenu(
            alignment = Alignment.BottomEnd,
            panelModifier = Modifier.padding(
                bottom = padding.calculateBottomPadding(),
                end = 4.dp
            ),
            onDismissRequest = { state.selectionMenuExpanded = false }
        ) {
            selectedEntries.singleOrNull()?.takeIf { it.isArchive && !it.isDirectory }
                ?.let { candidate ->
                    ZippestMenuItem(
                        text = { Text(context.getString(R.string.browser_preview)) },
                        leadingIcon = { Icon(Icons.Outlined.Visibility, null) },
                        onClick = {
                            state.selectionMenuExpanded = false
                            controller.clearSelection()
                            onOpenArchive(candidate)
                        }
                    )
                }
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_compress)) },
                leadingIcon = { Icon(Icons.Outlined.Unarchive, null) },
                onClick = {
                    state.selectionMenuExpanded = false
                    onCompressSelected()
                }
            )
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_trash)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = {
                    state.selectionMenuExpanded = false
                    controller.clearSelection()
                    state.showTrash = true
                }
            )
            selectedEntries.singleOrNull()?.takeIf { !it.isDirectory }?.let { candidate ->
                ZippestMenuItem(
                    text = { Text(context.getString(R.string.browser_verify)) },
                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null) },
                    onClick = {
                        state.selectionMenuExpanded = false
                        state.checksumTarget = candidate
                        state.checksumResult = null
                        state.checksumBusy = true
                        scope.launch {
                            state.checksumResult = com.decompress.app.data.Checksums
                                .compute(context, candidate.uri)
                            state.checksumBusy = false
                        }
                    }
                )
            }
            val repairCandidate = selectedEntries.singleOrNull()?.takeIf {
                it.uri.scheme == "file" &&
                    (it.name.endsWith(".zip", true) || it.name.endsWith(".apk", true))
            }
            if (repairCandidate != null) {
                ZippestMenuItem(
                    text = { Text(context.getString(R.string.browser_repair)) },
                    leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                    onClick = {
                        state.selectionMenuExpanded = false
                        val source = repairCandidate.uri.path?.let(::File)
                        if (source == null || !source.exists()) {
                            onNotify(context.getString(R.string.browser_unavailable))
                        } else {
                            onNotify(context.getString(R.string.browser_repairing))
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    val output = File(
                                        source.parentFile,
                                        source.nameWithoutExtension + ".repaired.zip"
                                    )
                                    com.decompress.app.data.ZipRepair.repair(source, output)
                                }
                                state.repairMessage = when {
                                    result == null -> context.getString(R.string.browser_repair_nothing)
                                    result.skipped > 0 ->
                                        context.getString(R.string.browser_restored_entries, result.entries, result.skipped, result.file.name)
                                    else ->
                                        context.getString(R.string.browser_restored_entries_simple, result.entries, result.file.name)
                                }
                                controller.refresh()
                            }
                        }
                    }
                )
            }
            val canOpenWith = selectedEntries.singleOrNull()?.let {
                !it.isDirectory && !it.isArchive
            } == true
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_open_with)) },
                leadingIcon = { Icon(Icons.Outlined.OpenInNew, null) },
                enabled = canOpenWith,
                onClick = {
                    state.selectionMenuExpanded = false
                    selectedEntries.singleOrNull()?.let { entry ->
                        if (!entry.isDirectory && !entry.isArchive) {
                            onOpenWithFile(entry)
                        }
                    }
                }
            )
            ZippestMenuItem(
                text = { Text(context.getString(R.string.browser_share)) },
                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                onClick = {
                    state.selectionMenuExpanded = false
                    val shared = shareEntries(context, selectedEntries)
                    if (!shared) onNotify(context.getString(R.string.browser_no_share_app))
                }
            )
        }
    }
}
