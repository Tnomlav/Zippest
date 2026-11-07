package com.decompress.app

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.decompress.app.ui.DeleteConfirmationDialog
import com.decompress.app.ui.theme.ZippestTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DialogViewsTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun deleteDialogShowsCountHintAndConfirms() {
        var confirmed = false
        rule.setContent {
            ZippestTheme {
                DeleteConfirmationDialog(
                    itemCount = 3,
                    useTrash = true,
                    onDismiss = {},
                    onConfirm = { confirmed = true }
                )
            }
        }
        rule.onNodeWithText("删除 3 项？").assertExists()
        rule.onNodeWithText("删除后可在回收站恢复").assertExists()
        rule.onNodeWithText("删除").performClick()
        assertTrue(confirmed)
    }
}
