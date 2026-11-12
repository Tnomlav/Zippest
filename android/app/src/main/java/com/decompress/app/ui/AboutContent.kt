package com.decompress.app.ui
import com.decompress.app.R

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
        UiStrings.get(R.string.about2_privacy_network),
        UiStrings.get(R.string.about2_privacy_network_body)
    ),
    PrivacySection(
        UiStrings.get(R.string.about2_privacy_local),
        UiStrings.get(R.string.about2_privacy_local_body)
    ),
    PrivacySection(
        UiStrings.get(R.string.about2_privacy_permissions),
        UiStrings.get(R.string.about2_privacy_permissions_body)
    ),
    PrivacySection(
        UiStrings.get(R.string.about2_privacy_clipboard),
        UiStrings.get(R.string.about2_privacy_clipboard_body)
    ),
    PrivacySection(
        UiStrings.get(R.string.about2_privacy_trash),
        "删除到回收站的文件、应用设置、后台任务记录和加密凭据均保存在本机应用数据区域。" +
            "回收站上限 2 GB，超出时会先清理最旧的记录。" +
            "注意：卸载 Zippest 会连同回收站一起删除，重要文件请先恢复到其他文件夹。" +
            "你可以通过回收站恢复或永久删除项目，也可在系统设置中清除应用数据。"
    ),
    PrivacySection(
        UiStrings.get(R.string.about2_privacy_third_party),
        UiStrings.get(R.string.about2_privacy_third_party_body)
    ),
    PrivacySection(
        UiStrings.get(R.string.about2_privacy_updates),
        UiStrings.get(R.string.about2_privacy_updates_body, ZIPPEST_PRIVACY_EFFECTIVE_DATE)
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
        components = UiStrings.get(R.string.about2_license_kotlin),
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "Apache Commons Compress 1.27.1",
        components = UiStrings.get(R.string.about2_license_tar),
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "Zip4j 2.11.5",
        components = UiStrings.get(R.string.about2_license_zip),
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "LZ4 Java 1.8.0",
        components = UiStrings.get(R.string.about2_license_lz4),
        license = "Apache License 2.0",
        licenseUrl = "https://www.apache.org/licenses/LICENSE-2.0"
    ),
    OpenSourceLicense(
        name = "JNA 5.17.0",
        components = UiStrings.get(R.string.about2_license_7zip),
        license = UiStrings.get(R.string.about2_license_lgpl),
        licenseUrl = "https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"
    ),
    OpenSourceLicense(
        name = "XZ for Java 1.10",
        components = UiStrings.get(R.string.about2_license_xz),
        license = "0BSD",
        licenseUrl = "https://opensource.org/license/0bsd"
    ),
    OpenSourceLicense(
        name = "Brotli Java Decoder 0.1.2",
        components = UiStrings.get(R.string.about2_license_brotli),
        license = "MIT License",
        licenseUrl = "https://github.com/google/brotli/blob/master/LICENSE"
    ),
    OpenSourceLicense(
        name = "junrar 8.1.2",
        components = UiStrings.get(R.string.about2_license_rar),
        license = "UnRAR License",
        licenseUrl = "https://github.com/junrar/junrar/blob/master/LICENSE"
    ),
    OpenSourceLicense(
        name = "libarchive for Android 1.1.4",
        components = UiStrings.get(R.string.about2_license_native),
        license = "Apache License 2.0 / BSD 2-Clause 及上游组件许可证",
        licenseUrl = "https://github.com/zhanghai/libarchive-android"
    )
)
