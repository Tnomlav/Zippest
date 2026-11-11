package com.decompress.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.decompress.app.data.FileBrowserController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun FileBrowserBottomBar(
    state: FileBrowserState,
    controller: FileBrowserController,
    selectedCount: Int,
    archiveTaskActive: Boolean,
    scope: CoroutineScope,
    onStartTransfer: (Boolean) -> Unit,
    onCancelTransfer: () -> Unit,
    onConfirmTransfer: () -> Unit,
    onExtract: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column {
        when {
            state.transferSession != null -> TransferToolbar(
                move = state.transferSession?.move == true,
                itemCount = state.transferSession?.sources?.size ?: 0,
                path = controller.path.joinToString(" / "),
                busy = state.transferBusy,
                canConfirm = controller.currentUri != null &&
                    controller.treeUri != null &&
                    (
                        state.transferSession?.move != true ||
                            controller.currentUri !=
                                state.transferSession?.sourceLocation?.currentUri
                        ),
                onCancel = onCancelTransfer,
                onConfirm = onConfirmTransfer
            )
            selectedCount > 0 -> SelectionToolbar(
                onExtract = onExtract,
                onCopy = { onStartTransfer(false) },
                onMove = { onStartTransfer(true) },
                onRename = onRename,
                onDelete = onDelete,
                menuExpanded = state.selectionMenuExpanded,
                onMenuExpandedChange = onMenuExpandedChange
            )
            !archiveTaskActive && controller.treeUri != null -> StorageBar(
                context = context,
                hasFullAccess =
                    controller.storageMode == FileBrowserController.StorageMode.DIRECT,
                onSwipeUpHome = {
                    scope.launch { controller.openInternalStorage() }
                }
            )
        }
    }
}
