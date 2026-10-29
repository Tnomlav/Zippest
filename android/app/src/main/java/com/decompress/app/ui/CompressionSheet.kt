@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.decompress.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.decompress.app.data.ArchiveCompressionLevel
import com.decompress.app.data.BrowserEntry
import com.decompress.app.data.CompressionFormat
import com.decompress.app.data.CompressionOptions
import com.decompress.app.data.archiveNameWithoutExtension
import com.decompress.app.service.CompressionScheduler
import com.decompress.app.service.CompressionTask

data class CompressTarget(
    val entries: List<BrowserEntry>,
    val sourceTreeUri: Uri,
    val parentUri: Uri,
    val parentTreeUri: Uri,
    val parentPath: String
)

private data class SplitOption(val label: String, val bytes: Long?)

private val splitOptions = listOf(
    SplitOption("不分卷", null),
    SplitOption("10 MB", 10L * 1024 * 1024),
    SplitOption("100 MB", 100L * 1024 * 1024),
    SplitOption("500 MB", 500L * 1024 * 1024),
    SplitOption("1 GB", 1024L * 1024 * 1024),
    SplitOption("2 GB", 2L * 1024 * 1024 * 1024)
)

@Composable
fun CompressionSheet(
    target: CompressTarget,
    onDismiss: () -> Unit,
    onStarted: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialBaseName = remember(target.entries) {
        when {
            target.entries.size > 1 -> "压缩包"
            target.entries.single().isDirectory -> target.entries.single().name
            else -> archiveNameWithoutExtension(target.entries.single().name)
        }
    }

    var format by rememberSaveable { mutableStateOf(CompressionFormat.ZIP) }
    var fileName by rememberSaveable { mutableStateOf("$initialBaseName.zip") }
    var level by rememberSaveable { mutableStateOf(ArchiveCompressionLevel.NORMAL) }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var splitOptionIndex by rememberSaveable { mutableStateOf(0) }
    val splitOption = splitOptions[splitOptionIndex]
    var customTreeUri by remember { mutableStateOf<Uri?>(null) }
    var customRootUri by remember { mutableStateOf<Uri?>(null) }
    var customName by remember { mutableStateOf<String?>(null) }
    var useCustomLocation by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notificationRequested by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            customTreeUri = uri
            customRootUri = treeRootDocumentUri(uri)
            customName = queryDisplayName(context, uri)
            useCustomLocation = true
        }
    }

    fun enqueue() {
        if (useCustomLocation && (customTreeUri == null || customRootUri == null)) {
            error = "请先选择保存位置"
            return
        }
        if (!format.acceptsMultipleSources &&
            (target.entries.size != 1 || target.entries.first().isDirectory)
        ) {
            error = "${format.label} 只能压缩单个文件"
            return
        }
        if (password.isNotEmpty() && !format.supportsPassword) {
            error = "${format.label} 不支持密码"
            return
        }
        val normalizedName = fileName.trim()
        if (normalizedName.isBlank()) {
            error = "请输入文件名"
            return
        }
        val outputName = if (normalizedName.lowercase().endsWith(format.extension)) {
            normalizedName
        } else {
            normalizedName + format.extension
        }
        error = null
        runCatching {
            CompressionScheduler.enqueue(
                context,
                CompressionTask(
                    sources = target.entries,
                    sourceTreeUri = target.sourceTreeUri,
                    targetParentUri = if (useCustomLocation) {
                        customRootUri!!
                    } else {
                        target.parentUri
                    },
                    targetTreeUri = if (useCustomLocation) {
                        customTreeUri!!
                    } else {
                        target.parentTreeUri
                    },
                    options = CompressionOptions(
                        format = format,
                        archiveName = outputName,
                        level = level,
                        password = password,
                        splitSizeBytes = splitOption.bytes
                    )
                )
            )
            onStarted()
        }.onFailure {
            error = it.message ?: "无法创建压缩任务"
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (notificationRequested) enqueue()
        notificationRequested = false
    }

    fun requestNotificationAndEnqueue() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationRequested = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enqueue()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "压缩文件",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "已选 ${target.entries.size} 项 · ${target.parentPath}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))
                SectionLabel("压缩格式")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompressionFormat.entries.forEach { item ->
                        val enabled = item.acceptsMultipleSources ||
                            (target.entries.size == 1 && !target.entries.first().isDirectory)
                        FilterChip(
                            selected = format == item,
                            enabled = enabled,
                            onClick = {
                                val previousSuffix = format.extension
                                val sourceFile = target.entries.singleOrNull()
                                    ?.takeUnless { it.isDirectory }
                                val base = if (
                                    sourceFile != null && !item.acceptsMultipleSources
                                ) {
                                    sourceFile.name
                                } else if (fileName.lowercase().endsWith(previousSuffix)) {
                                    fileName.dropLast(previousSuffix.length)
                                } else {
                                    archiveNameWithoutExtension(fileName)
                                }
                                format = item
                                fileName = base + item.extension
                                if (!item.supportsPassword) password = ""
                            },
                            label = { Text(item.label) }
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("文件名")
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("压缩包${format.extension}") }
                )

                Spacer(Modifier.height(14.dp))
                SectionLabel("压缩级别")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArchiveCompressionLevel.entries.forEach { item ->
                        FilterChip(
                            selected = level == item,
                            onClick = { level = item },
                            label = { Text(item.label) }
                        )
                    }
                }

                if (format.supportsPassword) {
                    Spacer(Modifier.height(14.dp))
                    SectionLabel("密码（可选）")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("不填写则不加密") },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Outlined.VisibilityOff
                                    else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) {
                                        "隐藏密码"
                                    } else {
                                        "显示密码"
                                    }
                                )
                            }
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))
                SectionLabel("分卷大小")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    splitOptions.forEach { option ->
                        FilterChip(
                            selected = splitOption == option,
                            onClick = { splitOptionIndex = splitOptions.indexOf(option) },
                            label = { Text(option.label) }
                        )
                    }
                }
                if (splitOption.bytes != null) {
                    Spacer(Modifier.height(6.dp))
                    val effectiveName = if (
                        fileName.trim().lowercase().endsWith(format.extension)
                    ) {
                        fileName.trim()
                    } else {
                        fileName.trim() + format.extension
                    }
                    val splitDescription = if (format == CompressionFormat.ZIP) {
                        val baseName = effectiveName.removeSuffix(".zip")
                        "$baseName.z01、$baseName.z02…$effectiveName；打开 .z01 或 .zip 均可解压"
                    } else {
                        "$effectiveName.001、.002…；在 Zippest 中打开 .001 即可解压"
                    }
                    Text(
                        splitDescription,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionLabel("保存到")
                CompressionChoice(
                    title = "当前文件夹",
                    detail = target.parentPath,
                    selected = !useCustomLocation,
                    onClick = { useCustomLocation = false }
                )
                Spacer(Modifier.height(8.dp))
                CompressionChoice(
                    title = "选择其他位置",
                    detail = customName ?: "选择手机中的任意文件夹",
                    selected = useCustomLocation && customTreeUri != null,
                    onClick = { folderPicker.launch(null) }
                )

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("取消")
                }
                Button(
                    onClick = { requestNotificationAndEnqueue() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("开始压缩")
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CompressionChoice(
    title: String,
    detail: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
