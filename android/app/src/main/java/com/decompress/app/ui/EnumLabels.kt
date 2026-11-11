package com.decompress.app.ui

import com.decompress.app.data.AppAccent
import com.decompress.app.data.AppLanguage
import com.decompress.app.data.BrowserSortMode
import com.decompress.app.data.ConflictPolicy
import com.decompress.app.data.DefaultDestinationMode
import com.decompress.app.data.ExtractSection
import com.decompress.app.data.FileNameEncoding
import com.decompress.app.data.RestoreConflictPolicy
import com.decompress.app.data.ThemeMode
import com.decompress.app.data.TrashQuota
import com.decompress.app.data.TrashRetention

/**
 * 枚举的界面文案（P0 先从 data 层搬出来，P2 抽取时替换为 stringResource）。
 * 数据层只保留值和行为，不再携带中文。
 */
internal fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "跟随系统"
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
}

internal fun AppLanguage.label(): String = when (this) {
    AppLanguage.SYSTEM -> "跟随系统"
    AppLanguage.CHINESE -> "中文"
    AppLanguage.ENGLISH -> "English"
}

internal fun DefaultDestinationMode.label(): String = when (this) {
    DefaultDestinationMode.NEW_FOLDER -> "同名文件夹"
    DefaultDestinationMode.CURRENT_FOLDER -> "当前文件夹"
    DefaultDestinationMode.CUSTOM -> "固定位置"
}

internal fun AppAccent.label(): String = when (this) {
    AppAccent.GREEN -> "森林绿"
    AppAccent.BLUE -> "海蓝"
    AppAccent.PURPLE -> "紫罗兰"
    AppAccent.AMBER -> "琥珀"
    AppAccent.ROSE -> "玫瑰"
    AppAccent.GRAPHITE -> "石墨"
}

internal fun ExtractSection.label(): String = when (this) {
    ExtractSection.PASSWORD -> "密码"
    ExtractSection.NAME -> "压缩包名称"
    ExtractSection.DESTINATION -> "解压位置"
    ExtractSection.CONFLICT -> "遇到同名文件"
    ExtractSection.ENCODING -> "文件名编码"
}

internal fun ExtractSection.detail(): String = when (this) {
    ExtractSection.PASSWORD -> "压缩包加密时填写"
    ExtractSection.NAME -> "解压前改包名和后缀"
    ExtractSection.DESTINATION -> "当前文件夹 / 同名文件夹 / 其他位置"
    ExtractSection.CONFLICT -> "覆盖 / 跳过 / 自动重命名"
    ExtractSection.ENCODING -> "自动 / GBK / Shift-JIS / UTF-8"
}

internal fun BrowserSortMode.label(): String = when (this) {
    BrowserSortMode.NAME -> "名称"
    BrowserSortMode.SIZE -> "大小"
    BrowserSortMode.DATE -> "日期"
    BrowserSortMode.TYPE -> "类型"
}

internal fun TrashRetention.label(): String = when (this) {
    TrashRetention.DAYS_7 -> "7 天"
    TrashRetention.DAYS_30 -> "30 天"
    TrashRetention.DAYS_90 -> "90 天"
    TrashRetention.FOREVER -> "永久保留"
}

internal fun TrashQuota.label(): String = when (this) {
    TrashQuota.MB_500 -> "500 MB"
    TrashQuota.GB_1 -> "1 GB"
    TrashQuota.GB_2 -> "2 GB"
    TrashQuota.GB_5 -> "5 GB"
    TrashQuota.UNLIMITED -> "不限"
}

internal fun RestoreConflictPolicy.label(): String = when (this) {
    RestoreConflictPolicy.RENAME -> "改名，全部留下"
    RestoreConflictPolicy.OVERWRITE -> "覆盖同名文件"
}

internal fun ConflictPolicy.label(): String = when (this) {
    ConflictPolicy.OVERWRITE -> "覆盖"
    ConflictPolicy.SKIP -> "跳过"
    ConflictPolicy.RENAME -> "自动重命名"
}

internal fun FileNameEncoding.label(): String = when (this) {
    FileNameEncoding.AUTO -> "自动"
    FileNameEncoding.GBK -> "GBK"
    FileNameEncoding.SHIFT_JIS -> "Shift-JIS"
    FileNameEncoding.UTF8 -> "UTF-8"
}
