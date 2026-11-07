package com.decompress.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun AboutInfoDialog(
    versionName: String,
    packageName: String,
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 Zippest") },
        text = {
            SelectionContainer {
                Column {
                    Text(
                        "Android 文件管理与压缩工具",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "版本：$versionName\n" +
                            "包名：$packageName\n" +
                            "最低系统：Android 8.0（API 26）\n" +
                            "开发者：Tnomlav",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "支持文件管理、批量密码解压、后缀快捷重命名、分卷、回收站、" +
                            "后台任务和本地压缩格式处理。",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "网络权限：仅检查更新\n" +
                            "Zippest 平时不联网；只有你点按版本号检查更新时，才会访问 GitHub Releases API 查询版本并下载安装包。" +
                            "文件、密码和文件名仍只在本机处理。可在 系统设置 → 应用 → Zippest → 权限 里看到网络权限。",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "开源许可：Apache-2.0",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { onOpenLink(ZIPPEST_LICENSE_URL) },
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text("查看项目许可证")
                    }
                    TextButton(
                        onClick = { onOpenLink(ZIPPEST_GITHUB_URL) },
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text("打开 GitHub 项目")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
internal fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("隐私政策") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Column {
                        Text(
                            "生效日期：$ZIPPEST_PRIVACY_EFFECTIVE_DATE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        zippestPrivacySections.forEach { section ->
                            Text(
                                section.title,
                                modifier = Modifier.padding(top = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                section.body,
                                modifier = Modifier.padding(top = 5.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
internal fun OpenSourceLicensesDialog(
    onOpenLink: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("第三方开源许可") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column {
                    Text(
                        "Zippest 使用以下开源组件。各组件版权归原作者所有，" +
                            "具体条款以对应许可证为准。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    zippestOpenSourceLicenses.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)
                            )
                        }
                        Text(
                            item.name,
                            modifier = Modifier.padding(top = if (index == 0) 14.dp else 0.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            item.components,
                            modifier = Modifier.padding(top = 3.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            item.license,
                            modifier = Modifier.padding(top = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { onOpenLink(item.licenseUrl) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 0.dp
                            )
                        ) {
                            Text("查看许可证")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
