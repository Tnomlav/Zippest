package com.decompress.app.data

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * 把显式选择的语言应用到 Context。
 * SYSTEM 原样返回，交给系统；CHINESE / ENGLISH 只覆盖该 Context 的 Configuration。
 */
object LanguageContext {

    fun wrap(base: Context, language: AppLanguage): Context {
        if (language == AppLanguage.SYSTEM) return base
        val tag = when (language) {
            AppLanguage.CHINESE -> "zh-CN"
            AppLanguage.ENGLISH -> "en"
            AppLanguage.SYSTEM -> return base
        }
        val locale = Locale.forLanguageTag(tag)
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }
}
