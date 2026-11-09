package com.decompress.app.ui

import android.os.Environment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import com.decompress.app.data.AppSettings
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.GlobalSearchHit
import com.decompress.app.data.TaskHistoryEntry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun FileBrowserContent(
    controller: FileBrowserController,
    settings: AppSettings,
    visibleEntries: List<BrowserEntry>,
    query: String,
    searchMode: Boolean,
    globalSearch: Boolean,
    globalHits: List<GlobalSearchHit>,
    globalSearching: Boolean,
    recentExtracts: List<TaskHistoryEntry>,
    pageSize: Int,
    onLoadMore: () -> Unit,
    transferActive: Boolean,
    transferBusy: Boolean,
    scope: CoroutineScope,
    onExtractFile: (BrowserEntry) -> Unit,
    onOpenArchive: (BrowserEntry) -> Unit,
    onOpenWithFile: (BrowserEntry) -> Unit,
    onGlobalHitOpen: (BrowserEntry) -> Unit,
    onBrowseDownloads: () -> Unit,
    onOpenHistoryLocation: (TaskHistoryEntry) -> Unit
) {
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val showGlobalResults = searchMode && globalSearch && query.isNotBlank()
    if (showGlobalResults) {
        when {
            globalSearching -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(10.dp))
                Text(
                    "正在搜索全部存储…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            globalHits.isEmpty() -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    EmptyState(
                        modifier = Modifier.fillParentMaxSize(),
                        filtered = true
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    top = 8.dp,
                    end = 12.dp,
                    bottom = 8.dp
                )
            ) {
                items(globalHits, key = { it.entry.uri.toString() }) { hit ->
                    GlobalSearchRow(
                        hit = hit,
                        onClick = {
                            if (hit.entry.isDirectory) {
                                hit.entry.uri.path?.let { path ->
                                    scope.launch {
                                        controller.openStorageRoot(File(path), hit.entry.name)
                                    }
                                }
                            } else {
                                onGlobalHitOpen(hit.entry)
                            }
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
                    )
                }
            }
        }
        return
    }

    if (visibleEntries.isEmpty()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                EmptyState(
                    modifier = Modifier.fillParentMaxSize(),
                    filtered = query.isNotBlank(),
                    onBrowseDownloads = onBrowseDownloads
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 12.dp,
            top = 8.dp,
            end = 12.dp,
            bottom = if (controller.selected.size > 0) 82.dp else 8.dp
        )
    ) {
        if (!searchMode &&
            settings.showRecentExtractions &&
            controller.path.size <= 1 &&
            recentExtracts.isNotEmpty()
        ) {
            item {
                RecentExtractionStrip(
                    entries = recentExtracts,
                    onOpen = { entry -> onOpenHistoryLocation(entry) }
                )
            }
        }
        if (visibleEntries.size > pageSize) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onLoadMore) {
                        Text("再显示 300 项（剩余 ${visibleEntries.size - pageSize}）")
                    }
                }
            }
        }
        val shownEntries = visibleEntries.take(pageSize)
        items(shownEntries, key = { it.uri.toString() }) { entry ->
            EntryRow(
                modifier = Modifier.animateItem(),
                entry = entry,
                selectionMode = controller.selectionMode,
                selected = entry.uri in controller.selected,
                showThumbnail = settings.showThumbnails,
                showArchivePreview = !transferActive,
                onClick = {
                    when {
                        transferBusy -> Unit
                        transferActive -> {
                            if (entry.isDirectory) {
                                scope.launch { controller.open(entry) }
                            }
                        }
                        controller.selectionMode -> controller.toggleSelection(entry.uri)
                        entry.isDirectory -> scope.launch { controller.open(entry) }
                        entry.isArchive -> onOpenArchive(entry)
                        else -> onOpenWithFile(entry)
                    }
                },
                onPreviewArchive = {
                    if (!transferActive && !transferBusy) onOpenArchive(entry)
                },
                onExtractFile = if (!entry.isDirectory && !entry.isArchive && !transferActive) {
                    { onExtractFile(entry) }
                } else {
                    null
                },
                onLongClick = {
                    if (!transferActive && !transferBusy) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        controller.startSelection(entry.uri)
                    }
                },
                onCheckedChange = {
                    if (!transferActive && !transferBusy) {
                        controller.toggleSelection(entry.uri)
                    }
                }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f)
            )
        }
    }
}
