package com.decompress.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.decompress.app.data.AppLanguage
import com.decompress.app.data.LanguageContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 语言入口的资源验证：不启动 Activity，只验证 LanguageContext 包装后的资源语言。
 * 覆盖 I18N-10 的 attachBaseContext 逻辑；系统级 LocaleManager 同步不在自动断言范围。
 */
@RunWith(AndroidJUnit4::class)
class LanguageSwitchTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun englishResourcesAfterSwitch() {
        val wrapped = LanguageContext.wrap(context, AppLanguage.ENGLISH)
        assertEquals("Open with Zippest", wrapped.getString(R.string.open_with_zippest))
        assertEquals("en", wrapped.resources.configuration.locales[0].language)
    }

    @Test
    fun chineseResourcesAfterSwitch() {
        val wrapped = LanguageContext.wrap(context, AppLanguage.CHINESE)
        assertEquals("用 Zippest 打开", wrapped.getString(R.string.open_with_zippest))
        assertEquals("zh", wrapped.resources.configuration.locales[0].language)
    }

    @Test
    fun systemLanguageKeepsDeviceDefault() {
        val wrapped = LanguageContext.wrap(context, AppLanguage.SYSTEM)
        assertEquals(
            context.getString(R.string.open_with_zippest),
            wrapped.getString(R.string.open_with_zippest)
        )
    }
}
