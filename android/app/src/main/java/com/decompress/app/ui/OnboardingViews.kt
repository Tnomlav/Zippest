package com.decompress.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun Onboarding(
    modifier: Modifier,
    deniedOnce: Boolean,
    onRequestAccess: () -> Unit,
    onChooseFolder: () -> Unit
) {
    Column(
        modifier = modifier.padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "访问手机文件",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "解压、压缩和整理文件都在这台手机上完成，过程中不需要联网。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        OnboardingBenefit("打开所有文件夹，不用每次选目录")
        OnboardingBenefit("文件只在本机读写，不上传任何内容")
        OnboardingBenefit("随时可以在系统设置里撤销")
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequestAccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("授予所有文件访问权限")
        }
        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = onChooseFolder,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("只选一个文件夹（更保守）")
        }
        if (deniedOnce) {
            Spacer(Modifier.height(6.dp))
            Text(
                "上次没有授权成功。可以再试一次，或先用文件夹模式——解压、压缩、搜索都能用。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Zippest 只在点按版本号检查更新时联网（访问 GitHub Releases API），文件始终只在本机处理；可在 系统设置 → 应用 → Zippest → 权限 里查看。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun OnboardingBenefit(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

