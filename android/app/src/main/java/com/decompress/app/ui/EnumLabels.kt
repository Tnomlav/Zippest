package com.decompress.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.decompress.app.R
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

/** 枚举的界面文案；走资源，中英文随应用语言切换。 */
@Composable
internal fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.enum_theme_system
        ThemeMode.LIGHT -> R.string.enum_theme_light
        ThemeMode.DARK -> R.string.enum_theme_dark
    }
)

@Composable
internal fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> R.string.enum_language_system
        AppLanguage.CHINESE -> R.string.enum_language_chinese
        AppLanguage.ENGLISH -> R.string.enum_language_english
    }
)

@Composable
internal fun DefaultDestinationMode.label(): String = stringResource(
    when (this) {
        DefaultDestinationMode.NEW_FOLDER -> R.string.enum_destination_new_folder
        DefaultDestinationMode.CURRENT_FOLDER -> R.string.enum_destination_current_folder
        DefaultDestinationMode.CUSTOM -> R.string.enum_destination_custom
    }
)

@Composable
internal fun AppAccent.label(): String = stringResource(
    when (this) {
        AppAccent.GREEN -> R.string.enum_accent_green
        AppAccent.BLUE -> R.string.enum_accent_blue
        AppAccent.PURPLE -> R.string.enum_accent_purple
        AppAccent.AMBER -> R.string.enum_accent_amber
        AppAccent.ROSE -> R.string.enum_accent_rose
        AppAccent.GRAPHITE -> R.string.enum_accent_graphite
    }
)

@Composable
internal fun ExtractSection.label(): String = stringResource(
    when (this) {
        ExtractSection.PASSWORD -> R.string.enum_section_password
        ExtractSection.NAME -> R.string.enum_section_name
        ExtractSection.DESTINATION -> R.string.enum_section_destination
        ExtractSection.CONFLICT -> R.string.enum_section_conflict
        ExtractSection.ENCODING -> R.string.enum_section_encoding
    }
)

@Composable
internal fun ExtractSection.detail(): String = stringResource(
    when (this) {
        ExtractSection.PASSWORD -> R.string.enum_section_password_detail
        ExtractSection.NAME -> R.string.enum_section_name_detail
        ExtractSection.DESTINATION -> R.string.enum_section_destination_detail
        ExtractSection.CONFLICT -> R.string.enum_section_conflict_detail
        ExtractSection.ENCODING -> R.string.enum_section_encoding_detail
    }
)

@Composable
internal fun BrowserSortMode.label(): String = stringResource(
    when (this) {
        BrowserSortMode.NAME -> R.string.enum_sort_name
        BrowserSortMode.SIZE -> R.string.enum_sort_size
        BrowserSortMode.DATE -> R.string.enum_sort_date
        BrowserSortMode.TYPE -> R.string.enum_sort_type
    }
)

@Composable
internal fun TrashRetention.label(): String = stringResource(
    when (this) {
        TrashRetention.DAYS_7 -> R.string.enum_retention_7
        TrashRetention.DAYS_30 -> R.string.enum_retention_30
        TrashRetention.DAYS_90 -> R.string.enum_retention_90
        TrashRetention.FOREVER -> R.string.enum_retention_forever
    }
)

@Composable
internal fun TrashQuota.label(): String = stringResource(
    when (this) {
        TrashQuota.MB_500 -> R.string.enum_quota_500mb
        TrashQuota.GB_1 -> R.string.enum_quota_1gb
        TrashQuota.GB_2 -> R.string.enum_quota_2gb
        TrashQuota.GB_5 -> R.string.enum_quota_5gb
        TrashQuota.UNLIMITED -> R.string.enum_quota_unlimited
    }
)

@Composable
internal fun RestoreConflictPolicy.label(): String = stringResource(
    when (this) {
        RestoreConflictPolicy.RENAME -> R.string.enum_restore_rename
        RestoreConflictPolicy.OVERWRITE -> R.string.enum_restore_overwrite
    }
)

@Composable
internal fun ConflictPolicy.label(): String = stringResource(
    when (this) {
        ConflictPolicy.OVERWRITE -> R.string.enum_conflict_overwrite
        ConflictPolicy.SKIP -> R.string.enum_conflict_skip
        ConflictPolicy.RENAME -> R.string.enum_conflict_rename
    }
)

@Composable
internal fun FileNameEncoding.label(): String = stringResource(
    when (this) {
        FileNameEncoding.AUTO -> R.string.enum_encoding_auto
        FileNameEncoding.GBK -> R.string.enum_encoding_gbk
        FileNameEncoding.SHIFT_JIS -> R.string.enum_encoding_shift_jis
        FileNameEncoding.UTF8 -> R.string.enum_encoding_utf8
    }
)
