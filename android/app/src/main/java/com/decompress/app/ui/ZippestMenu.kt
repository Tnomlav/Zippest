package com.decompress.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@Composable
internal fun ZippestDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 6.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 220.dp, max = 340.dp),
        offset = offset,
        shape = RoundedCornerShape(18.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        ),
        content = content
    )
}

@Composable
internal fun ZippestMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false
) {
    DropdownMenuItem(
        text = {
            androidx.compose.material3.ProvideTextStyle(
                MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                ),
                content = text
            )
        },
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .heightIn(min = 50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f)
                } else {
                    Color.Transparent
                }
            ),
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    )
}
