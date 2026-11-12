package com.decompress.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.decompress.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.decompress.app.ui.theme.ZippestRadius
import com.decompress.app.ui.theme.ZippestSpacing

/** 宽屏（≥840 dp）第三栏：当前文件夹的信息。 */
@Composable
internal fun BrowserInfoPane(
    folderName: String,
    path: List<String>,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    InfoPaneScaffold(title = UiStrings.get(R.string.info_current), modifier = modifier) {
        InfoLine(UiStrings.get(R.string.info_name), folderName.ifBlank { UiStrings.get(R.string.info_internal) })
        InfoLine(UiStrings.get(R.string.info_path), path.joinToString(" / ").ifBlank { UiStrings.get(R.string.info_internal) })
        InfoLine(UiStrings.get(R.string.info_items), UiStrings.get(R.string.info_count, itemCount))
    }
}

/** 宽屏（≥840 dp）第三栏：当前压缩包的信息。 */
@Composable
internal fun ArchiveInfoPane(
    archiveName: String,
    sizeText: String,
    entryCount: Int,
    encodingLabel: String,
    modifier: Modifier = Modifier
) {
    InfoPaneScaffold(title = UiStrings.get(R.string.info_archive), modifier = modifier) {
        InfoLine(UiStrings.get(R.string.info_name), archiveName)
        InfoLine(UiStrings.get(R.string.info_size), sizeText)
        InfoLine(UiStrings.get(R.string.info_entries), UiStrings.get(R.string.info_count, entryCount))
        InfoLine(UiStrings.get(R.string.info_encoding), encodingLabel)
    }
}

@Composable
internal fun InfoPaneScaffold(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .padding(
                start = ZippestSpacing.sm,
                end = ZippestSpacing.md,
                top = ZippestSpacing.md
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(ZippestRadius.md)
            )
            .padding(ZippestSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ZippestSpacing.sm)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
internal fun InfoLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
