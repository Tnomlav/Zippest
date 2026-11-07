package com.decompress.app.ui

internal const val ZIPPEST_GITHUB_URL = "https://github.com/Tnomlav/Zippest"
internal const val ZIPPEST_LICENSE_URL = "$ZIPPEST_GITHUB_URL/blob/main/LICENSE"
internal const val ZIPPEST_PRIVACY_EFFECTIVE_DATE = "2026-11-04"

internal data class PrivacySection(
    val title: String,
    val body: String
)

internal data class OpenSourceLicense(
    val name: String,
    val components: String,
    val license: String,
    val licenseUrl: String
)

internal val zippestPrivacySections = listOf(
    PrivacySection(
        "数据收集与网络",
        "Zippest 不要求注册账号，不包含广告、用户追踪或统计分析功能。应用持有网络权限，但只在两种情况下使用：你点按版本号检查更新（访问 GitHub Releases API），以及你主动用系统浏览器打开项目或许可证链接。除此之外，Zippest 不会向服务器上传文件、文件名、密码或其他使用信息。"
    ),
    PrivacySection(
        "本地文件处理",
        "文件浏览、压缩、解压、复制、移动、重命名、预览和完整性检查均在你的设备本地完成。只有你主动使用“打开方式”或“分享”时，所选文件或临时授权链接才会交给相应系统应用处理。"
    ),
    PrivacySection(
        "权限用途",
        "所有文件访问权限用于直接浏览和修改内部存储；Storage Access Framework 用于用户选择的文件夹授权；通知、前台服务和唤醒锁用于在后台或熄屏时继续压缩、解压任务并显示进度。Zippest 不会使用这些权限进行与所选操作无关的数据收集。"
    ),
    PrivacySection(
        "剪贴板与密码",
        "仅在解压设置或你主动点击剪贴板入口时读取当前剪贴板内容。密码可保存在内存或加密后的后台任务参数中，不会上传。应用不会在后台持续监听或记录剪贴板变化。"
    ),
    PrivacySection(
        "回收站与本地记录",
        "删除到回收站的文件、应用设置、后台任务记录和加密凭据均保存在本机应用数据区域。" +
            "回收站上限 2 GB，超出时会先清理最旧的记录。" +
            "注意：卸载 Zippest 会连同回收站一起删除，重要文件请先恢复到其他文件夹。" +
            "你可以通过回收站恢复或永久删除项目，也可在系统设置中清除应用数据。"
    ),
    PrivacySection(
        "第三方组件",
        "压缩与解压功能使用本地开源库。第三方组件仅在本机执行文件和压缩格式处理，不提供用户追踪或远程数据收集能力。"
    ),
    PrivacySection(
        "政策更新与联系",
        "此政策自 $ZIPPEST_PRIVACY_EFFECTIVE_DATE 生效。若后续功能引入联网、云服务或数据收集，将同步更新政策。问题可通过 GitHub Issues 联系项目维护者。"
    )
)

internal val zippestOpenSourceLicenses = listOf(
    OpenSourceLicense(
        name = "AndroidX · Jetpack Compose · WorkManager",
        components = "Core KTX、Activity Compose、Compose、WorkManager、DocumentFile",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "Kotlin · kotlinx.coroutines",
        components = "Kotlin 标准库与 Android 协程支持",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "Apache Commons Compress 1.27.1",
        components = "TAR、GZIP、BZIP2、CPIO、LZMA、Zstandard 等流式压缩格式",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "Zip4j 2.11.5",
        components = "ZIP 加密、压缩与分卷支持",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "LZ4 Java 1.8.0",
        components = "LZ4 压缩与解压",
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "JNA 5.17.0",
        components = "7-Zip 原生库绑定",
        license = "LGPL-2.1-or-later 或 Apache License 2.0",
        licenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
    ),
    OpenSourceLicense(
        name = "XZ for Java 1.10",
        components = "XZ 与 LZMA2 解压支持",
        license = "0BSD",
        licenseUrl = "https://opensource.org/license/0bsd"
    ),
    OpenSourceLicense(
        name = "Brotli Java Decoder 0.1.2",
        components = "Brotli 解压支持",
        license = "MIT License",
        licenseUrl = "https://github.com/google/brotli/blob/master/LICENSE"
    ),
    OpenSourceLicense(
        name = "junrar 8.1.2",
        components = "RAR4 与 RAR5 解压支持",
        license = "UnRAR License",
        licenseUrl = "https://github.com/junrar/junrar/blob/master/LICENSE"
    ),
    OpenSourceLicense(
        name = "libarchive for Android 1.1.4",
        components = "NDK 原生 7Z、RAR4、RAR5 解压，内置 bzip2、XZ、LZ4、Zstandard 与 Mbed TLS",
        license = "Apache License 2.0 / BSD 2-Clause 及上游组件许可证",
        licenseUrl = "https://github.com/zhanghai/libarchive-android"
    )
)
