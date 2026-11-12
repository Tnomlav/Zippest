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
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.decompress.app.R

@Composable
internal fun AboutInfoDialog(
    versionName: String,
    packageName: String,
    onOpenLink: (String) -> Unit,
    onOpenLocalLicense: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            SelectionContainer {
                Column {
                    Text(
                        UiStrings.get(R.string.about2_about_tool_line),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        UiStrings.get(R.string.about2_about_version_block, versionName, packageName),
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "压缩包工具：批量密码解压、后缀快捷重命名、分卷、回收站、" +
                            "后台任务和本地压缩格式处理；文件浏览与管理是顺带能做的事。",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        UiStrings.get(R.string.about2_about_network_title) + "\n" +
                            "Zippest 平时不联网；只有你点按版本号检查更新时，才会访问 GitHub Releases API 查询版本并下载安装包。" +
                            "文件、密码和文件名仍只在本机处理。可在 系统设置 → 应用 → Zippest → 权限 里看到网络权限。",
                        modifier = Modifier.padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        UiStrings.get(R.string.about2_about_license_title),
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = onOpenLocalLicense,
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text(stringResource(R.string.about_license_offline))
                    }
                    TextButton(
                        onClick = { onOpenLink(ZIPPEST_LICENSE_URL) },
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text(stringResource(R.string.about_view_license))
                    }
                    TextButton(
                        onClick = { onOpenLink(ZIPPEST_GITHUB_URL) },
                        modifier = Modifier.padding(top = 0.dp)
                    ) {
                        Text(stringResource(R.string.about_open_github))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
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
        title = { Text(stringResource(R.string.about_privacy_title)) },
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
                            UiStrings.get(R.string.about2_about_effective, ZIPPEST_PRIVACY_EFFECTIVE_DATE),
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
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
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
        title = { Text(stringResource(R.string.about_third_party_title)) },
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
                            modifier = Modifier.padding(top = if (index == 0) 16.dp else 0.dp),
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
                            Text(stringResource(R.string.about_view_license_short))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        }
    )
}

/**
 * 读取打包在 assets/legal/ 下的许可证原文。
 * 断网时关于页的“查看项目许可证”链接打不开，这里给一份随包的离线副本（第 5 章清单第 18 项）。
 */
@Composable
internal fun LegalTextDialog(
    title: String,
    assetName: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val body = remember(assetName) {
        runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.getOrElse { UiStrings.get(R.string.about2_about_read_failed) + assetName + "）。" }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.about_close))
            }
        }
    )
}
