package com.decompress.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileBrowserTopBar(
    selectionMode: Boolean,
    searchMode: Boolean,
    transferActive: Boolean,
    directStorage: Boolean,
    hasTree: Boolean,
    folderName: String,
    storageExpanded: Boolean,
    query: String,
    globalSearch: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    canGoUp: Boolean,
    searchFocus: FocusRequester,
    onClearSelection: () -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onExitSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onToggleGlobalSearch: () -> Unit,
    onGoUp: () -> Unit,
    onToggleQuickAccess: () -> Unit,
    onCancelTransfer: () -> Unit,
    onSearch: () -> Unit,
    onNewFolder: () -> Unit,
    onMore: () -> Unit,
    onOpenSettings: () -> Unit
) {
    when {
        selectionMode -> {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onClearSelection) {
                        Icon(Icons.Outlined.Close, contentDescription = "退出多选")
                    }
                },
                title = {
                    Column {
                        Text(
                            "已选 $selectedCount 项",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (selectedCount == 0) "取消选择以返回" else "点击项目继续选择",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = onSelectAll,
                        modifier = Modifier.size(48.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            )
        }
        searchMode -> {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onExitSearch) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "退出搜索")
                    }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocus),
                        singleLine = true,
                        placeholder = {
                            Text(if (globalSearch) "搜索全部存储" else "搜索当前文件夹")
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                },
                actions = {
                    TextButton(onClick = onToggleGlobalSearch) {
                        Text(if (globalSearch) "全部" else "当前")
                    }
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Outlined.Close, contentDescription = "清除搜索")
                        }
                    }
                }
            )
        }
        else -> {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onGoUp, enabled = canGoUp) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回上一级")
                    }
                },
                title = {
                    if (!hasTree) {
                        Text("Zippest", style = MaterialTheme.typography.titleLarge)
                    } else {
                        QuickAccessTopTitle(
                            directStorage = directStorage,
                            folderName = folderName,
                            expanded = storageExpanded,
                            onClick = onToggleQuickAccess
                        )
                    }
                },
                actions = {
                    if (transferActive) {
                        IconButton(onClick = onCancelTransfer) {
                            Icon(Icons.Outlined.Close, contentDescription = "取消复制或移动")
                        }
                    } else if (hasTree) {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Outlined.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = onNewFolder) {
                            Icon(Icons.Outlined.Add, contentDescription = "新建文件夹")
                        }
                        IconButton(onClick = onMore) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                        }
                    } else {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Outlined.Tune, contentDescription = "设置")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}
