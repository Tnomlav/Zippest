package com.decompress.app.ui

import android.content.Context
import androidx.annotation.StringRes

/**
 * 进程级当前本地化 Context。MainActivity.attachBaseContext 安装，
 * 让非 Composable 的回调也能直接取资源，避免到处传 Context。
 */
object UiStrings {
    @Volatile
    private var context: Context? = null

    fun install(value: Context) {
        context = value
    }

    fun get(@StringRes id: Int, vararg args: Any): String =
        requireNotNull(context) { "UiStrings is not installed" }.getString(id, *args)
}
