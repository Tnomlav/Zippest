package com.decompress.app.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.decompress.app.MainActivity

/**
 * 任务完成后的“打开结果目录”入口。
 *
 * 通知栏 action、主界面 Snackbar 都通过 ACTION_OPEN_LOCATION 把定位信息交回 MainActivity，
 * 由主界面把文件浏览器导航到该目录，避免“解压完了不知道文件在哪”。
 */
object TaskNavigation {
    const val ACTION_OPEN_LOCATION = "com.decompress.app.action.OPEN_LOCATION"
    const val EXTRA_LOCATION_URI = "location_uri"
    const val EXTRA_LOCATION_NAME = "location_name"
    const val EXTRA_LOCATION_TREE_URI = "location_tree_uri"
    const val EXTRA_LOCATION_PATH = "location_path"

    fun intent(
        context: Context,
        location: Uri,
        name: String,
        treeUri: Uri?,
        path: String
    ): Intent = Intent(context, MainActivity::class.java).apply {
        action = ACTION_OPEN_LOCATION
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(EXTRA_LOCATION_URI, location.toString())
        putExtra(EXTRA_LOCATION_NAME, name)
        putExtra(EXTRA_LOCATION_TREE_URI, treeUri?.toString())
        putExtra(EXTRA_LOCATION_PATH, path)
    }

    fun pendingIntent(
        context: Context,
        location: Uri,
        name: String,
        treeUri: Uri?,
        path: String
    ): PendingIntent = PendingIntent.getActivity(
        context.applicationContext,
        location.hashCode() xor name.hashCode(),
        intent(context, location, name, treeUri, path),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
