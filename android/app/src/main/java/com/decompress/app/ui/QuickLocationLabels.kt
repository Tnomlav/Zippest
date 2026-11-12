package com.decompress.app.ui

import com.decompress.app.R
import com.decompress.app.data.QuickLocation

/**
 * 内置快捷位置的显示名按当前语言取资源；自定义位置仍用用户填写的名称。
 * 数据层保存的 label 可能是历史中文，不参与内置项的显示。
 */
internal fun QuickLocation.displayLabel(): String = when (id) {
    "root" -> UiStrings.get(R.string.info_internal)
    "downloads" -> UiStrings.get(R.string.downloads)
    "pictures" -> UiStrings.get(R.string.gallery_short)
    "camera" -> UiStrings.get(R.string.camera)
    "movies" -> UiStrings.get(R.string.movies)
    "documents" -> UiStrings.get(R.string.documents_short)
    else -> label
}
