package com.decompress.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decompress.app.ui.SelectionToolbar
import com.decompress.app.ui.theme.ZippestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SelectionToolbarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun actionsAreClickable() {
        var copied = false
        rule.setContent {
            ZippestTheme {
                SelectionToolbar(
                    onExtract = {},
                    onCopy = { copied = true },
                    onMove = {},
                    onRename = {},
                    onDelete = {},
                    menuExpanded = false,
                    onMenuExpandedChange = {}
                )
            }
        }
        rule.onNodeWithText("复制").assertExists()
        rule.onNodeWithText("移动").assertExists()
        rule.onNodeWithText("删除").assertExists()
        rule.onNodeWithText("复制").performClick()
        assertTrue(copied)
    }
}
