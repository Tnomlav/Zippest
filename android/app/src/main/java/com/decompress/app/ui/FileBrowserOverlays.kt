package com.decompress.app.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.decompress.app.data.FileBrowserController
import com.decompress.app.data.QuickLocationRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun BoxScope.FileBrowserOverlays(
    state: FileBrowserState,
    controller: FileBrowserController,
    quickLocationRepository: QuickLocationRepository,
    padding: PaddingValues,
    selectedCount: Int,
    scope: CoroutineScope,
    onExtractSelected: () -> Unit,
    onBack: () -> Unit,
    onNotify: (String) -> Unit,
    onRefreshExternalStorages: () -> Unit,
    onClosePanels: () -> Unit,
    onDismissDialogs: () -> Unit
) {
    val context = LocalContext.current
    EdgeSwipeBackZones(
        onBack = onBack,
        onHome = {
            scope.launch {
                state.storageExpanded = false
                controller.clearSelection()
                controller.openInternalStorage()
            }
        }
    )
    if (
        state.storageExpanded &&
        controller.storageMode == FileBrowserController.StorageMode.DIRECT
    ) {
        StorageQuickLocationPanel(
            topInset = padding.calculateTopPadding(),
            currentUri = controller.currentUri,
            locations = quickLocationRepository.locations,
            externalStorages = state.externalStorages,
            onDismiss = { state.storageExpanded = false },
            onAdd = {
                onClosePanels()
                onDismissDialogs()
                state.showQuickLocationEditor = true
            },
            onOpen = { location ->
                state.storageExpanded = false
                scope.launch {
                    val opened = controller.openFixedLocation(location.label, location.path)
                    if (!opened) onNotify("${location.label}不可用")
                }
            },
            onOpenExternal = { storage ->
                state.storageExpanded = false
                scope.launch {
                    val opened = controller.openStorageRoot(File(storage.path), storage.label)
                    if (!opened) {
                        onNotify("${storage.label}不可用")
                        onRefreshExternalStorages()
                    }
                }
            }
        )
    }
}
