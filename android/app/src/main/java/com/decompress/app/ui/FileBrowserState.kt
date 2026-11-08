package com.decompress.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.GlobalSearchHit

internal class FileBrowserState {
    val _entryPageSize = mutableStateOf(300)
    var entryPageSize by _entryPageSize
    val _globalHits = mutableStateOf<List<GlobalSearchHit>>(emptyList<GlobalSearchHit>())
    var globalHits by _globalHits
    val _globalSearching = mutableStateOf(false)
    var globalSearching by _globalSearching
    val _pullRefreshing = mutableStateOf(false)
    var pullRefreshing by _pullRefreshing
    val _externalStorages = mutableStateOf<List<ExternalStorageLocation>>(emptyList<ExternalStorageLocation>())
    var externalStorages by _externalStorages
    val _storageExpanded = mutableStateOf(false)
    var storageExpanded by _storageExpanded
    val _sortExpanded = mutableStateOf(false)
    var sortExpanded by _sortExpanded
    val _moreExpanded = mutableStateOf(false)
    var moreExpanded by _moreExpanded
    val _selectionMenuExpanded = mutableStateOf(false)
    var selectionMenuExpanded by _selectionMenuExpanded
    val _showNewFolderDialog = mutableStateOf(false)
    var showNewFolderDialog by _showNewFolderDialog
    val _newFolderName = mutableStateOf("")
    var newFolderName by _newFolderName
    val _showRenameDialog = mutableStateOf(false)
    var showRenameDialog by _showRenameDialog
    val _batchPattern = mutableStateOf("{name}_{001}")
    var batchPattern by _batchPattern
    val _showBatchRename = mutableStateOf(false)
    var showBatchRename by _showBatchRename
    val _batchRenaming = mutableStateOf(false)
    var batchRenaming by _batchRenaming
    val _checksumTarget = mutableStateOf<BrowserEntry?>(null)
    var checksumTarget by _checksumTarget
    val _checksumResult = mutableStateOf<Pair<String, String>?>(null)
    var checksumResult by _checksumResult
    val _checksumBusy = mutableStateOf(false)
    var checksumBusy by _checksumBusy
    val _repairMessage = mutableStateOf<String?>(null)
    var repairMessage by _repairMessage
    val _renameValue = mutableStateOf(TextFieldValue())
    var renameValue by _renameValue
    val _showCustomSuffixDialog = mutableStateOf(false)
    var showCustomSuffixDialog by _showCustomSuffixDialog
    val _showDeleteConfirmation = mutableStateOf(false)
    var showDeleteConfirmation by _showDeleteConfirmation
    val _showTrash = mutableStateOf(false)
    var showTrash by _showTrash
    val _showQuickLocationEditor = mutableStateOf(false)
    var showQuickLocationEditor by _showQuickLocationEditor
    val _customSuffix = mutableStateOf("")
    var customSuffix by _customSuffix
    val _showTaskQueue = mutableStateOf(false)
    var showTaskQueue by _showTaskQueue
    val _transferSession = mutableStateOf<TransferSession?>(null)
    var transferSession by _transferSession
    val _transferBusy = mutableStateOf(false)
    var transferBusy by _transferBusy
    val _compressTarget = mutableStateOf<CompressTarget?>(null)
    var compressTarget by _compressTarget
    val _openWithRequest = mutableStateOf<OpenWithRequest?>(null)
    var openWithRequest by _openWithRequest
}
