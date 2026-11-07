package com.decompress.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposeSmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstRunShowsOnboarding() {
        rule.onNodeWithText("访问手机文件").assertExists()
        rule.onNodeWithText("只在本机读写，不联网、不上传").assertExists()
        rule.onNodeWithText("授予所有文件访问权限").assertExists()
    }
}
