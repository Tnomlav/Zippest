package com.decompress.app.data

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/**
 * 文件名的跨文字处理：NFC 归一化、搜索折叠、bidi 清洗、按码点截断。
 *
 * 这些规则和服务端/系统无关，只关心「任意语言文字的文件名都能正确显示、比较、搜索」。
 */
object NameText {

    /** 统一成 NFC，避免 macOS 的 NFD 文件名和 Android 的 NFC 文件名被当成两个。 */
    fun nfc(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFC)

    /** 文件名比较：NFC 之后忽略大小写。 */
    fun sameName(a: String, b: String): Boolean =
        nfc(a).equals(nfc(b), ignoreCase = true)

    /**
     * 搜索用的折叠形式：NFC → 去掉组合附加符号（café → cafe）→ 小写。
     * 用 Locale.ROOT，避免土耳其语 i/İ 之类的大小写陷阱。
     */
    fun foldForSearch(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")
            .lowercase(Locale.ROOT)

    /**
     * 去掉 bidi 方向控制符（U+202A–202E、U+2066–2069、U+200E/200F）。
     * 这些字符会让 `evil\u202Etxt.exe` 显示成 `evilexe.txt`，必须落盘前清洗。
     */
    fun sanitizeBidi(value: String): String =
        value.filterNot { char ->
            char in '\u202A'..'\u202E' ||
                char in '\u2066'..'\u2069' ||
                char == '\u200E' ||
                char == '\u200F'
        }

    /** 按 Unicode 码点截断，避免把 emoji / 代理对截成半个字符。 */
    fun truncateByCodePoints(value: String, maxCodePoints: Int): String {
        if (maxCodePoints <= 0 || value.isEmpty()) return ""
        if (value.codePointCount(0, value.length) <= maxCodePoints) return value
        val end = value.offsetByCodePoints(0, maxCodePoints)
        return value.substring(0, end)
    }

    /** 是否包含阿拉伯 / 希伯来等从右到左文字；这些名字在单行里更容易被省略号截断。 */
    fun hasRtlScript(value: String): Boolean = value.any { char ->
        char in '֐'..'׿' ||   // Hebrew
            char in '؀'..'ۿ' || // Arabic
            char in 'ݐ'..'ݿ' || // Arabic Supplement
            char in 'ࢠ'..'ࣿ' || // Arabic Extended-A
            char in 'יִ'..'﷿' || // Hebrew/Arabic presentation forms
            char in 'ﹰ'..'﻾'
    }

    /** 语言相关的排序器；每个调用方各自持有，Collator 不是线程安全的。 */
    fun collator(locale: Locale = Locale.getDefault()): Collator =
        Collator.getInstance(locale).apply {
            strength = Collator.SECONDARY
        }

    /** 只按文件名排序（不含目录优先），用于「类型/大小/日期」排序的次级比较。 */
    fun nameComparator(locale: Locale = Locale.getDefault()): Comparator<String> {
        val collator = collator(locale)
        return Comparator { left, right -> collator.compare(nfc(left), nfc(right)) }
    }

    /** 目录在前，其余按当前语言的 Collator 排序。 */
    fun comparator(locale: Locale = Locale.getDefault()): Comparator<BrowserEntry> {
        val collator = collator(locale)
        return Comparator { left, right ->
            if (left.isDirectory != right.isDirectory) {
                return@Comparator if (left.isDirectory) -1 else 1
            }
            collator.compare(nfc(left.name), nfc(right.name))
        }
    }

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
}
