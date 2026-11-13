package com.decompress.app.data

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 诊断导出（opt-in）：只包含版本、机型、设置摘要与任务数量，
 * 不包含文件内容、文件名、密码或剪贴板。
 */
object DiagnosticsExport {

    fun buildText(context: Context): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val settings = AppSettingsRepository.read(context)
        val history = runCatching { TaskHistoryStore(context.applicationContext).list() }
            .getOrDefault(emptyList())
        val locale = Locale.getDefault()
        val fontScale = context.resources.configuration.fontScale
        val builder = StringBuilder()
        builder.appendLine("Zippest diagnostics")
        builder.appendLine("generated: " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date()))
        builder.appendLine("version: " + (packageInfo?.versionName ?: "?") + " (" + (packageInfo?.longVersionCode ?: 0) + ")")
        builder.appendLine("package: " + context.packageName)
        builder.appendLine("device: " + Build.MANUFACTURER + " " + Build.MODEL)
        builder.appendLine("android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")")
        builder.appendLine("locale: " + locale.toLanguageTag() + ", fontScale=" + fontScale)
        builder.appendLine("language: " + settings.language.name)
        builder.appendLine("theme: " + settings.themeMode.name + ", accent=" + settings.accentColor.name)
        builder.appendLine("sort: " + settings.defaultSortMode.name + ", ascending=" + settings.sortAscending)
        builder.appendLine("destination: " + settings.defaultDestinationMode.name + ", conflict=" + settings.defaultConflictPolicy.name)
        builder.appendLine("encoding: " + settings.defaultEncoding.name)
        builder.appendLine("trash: enabled=" + settings.useTrash + ", retention=" + settings.trashRetention.name + ", quota=" + settings.trashQuota.name)
        builder.appendLine("thumbnails: " + settings.showThumbnails + ", hiddenFiles: " + settings.showHiddenFiles)
        builder.appendLine("taskHistory: " + history.size + " records")
        val succeeded = history.count { it.success }
        val failed = history.size - succeeded
        builder.appendLine("taskResults: ok=" + succeeded + ", failed=" + failed)
        builder.appendLine("NOTE: no file names, contents, passwords or clipboard data are included.")
        return builder.toString()
    }

    fun share(context: Context): Boolean = runCatching {
        val file = File(context.cacheDir, "zippest-diagnostics.txt")
        file.writeText(buildText(context))
        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Zippest diagnostics")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Zippest diagnostics"))
    }.isSuccess
}
