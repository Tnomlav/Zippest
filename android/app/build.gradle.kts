import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseSigningProperties = Properties().apply {
    val propertiesFile = rootProject.file("../.signing/zippest-release.properties")
    if (propertiesFile.exists()) {
        propertiesFile.inputStream().use(::load)
    }
}
val hasReleaseSigning = releaseSigningProperties.isNotEmpty()
// 闂佸搫鐗滈崜娆忥耿閺夋嚦鐔煎焺閸愩劎褰查梺闈╃秵閻撳妲愬?6_64闂佹寧绋戦ˇ铏閸垺瀚柛鎰典簼閺嗗繘鏌?PzippestEmulator闂佹寧绋戦懟顖濄亹瑜戦妵鎰板础閻愬瓨鍎ラ柣鐘差儏閸熸娊鎯侀鐐插嚑闁告洦鍋嗙槐锕傛煏?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 86
        versionName = "1.11.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 婵炴挻纰嶇换鍡欑矉閸℃稒鏅柛锔界磻A / libarchive闂佹寧绋戦ˇ顖炴偨椤愶附鍎?mips闂侀潧妫旂花?6 缂備焦绋戦ˇ鐢搞€傞埡鍐＝闁哄稁鍓涘Σ顖炲箹鐎涙ɑ灏紒鏃€娼欓銉р偓锝傛櫇閻熸繈鏌熼崹顐ｅ磩缂佺粯锕㈠畷鐘诲川椤愮喎浜?
        ndk {
            abiFilters += if (emulatorBuild) {
                listOf("x86_64")
            } else {
                listOf("arm64-v8a", "armeabi-v7a")
            }
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(
                    "../.signing/${releaseSigningProperties.getProperty("storeFile")}"
                )
                storePassword = releaseSigningProperties.getProperty("storePassword")
                keyAlias = releaseSigningProperties.getProperty("keyAlias")
                keyPassword = releaseSigningProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 闂佸憡鐟禍娆戞崲濮樿埖鍋?ARM 闁荤姳鐒﹂崕鎶剿囬鍕剳闁绘柨澹婇崝鈧梺娲绘娇閸斿矂宕哄畝鍕剭闁告洦浜ｉ埛鍫㈢磼?ABI闂佹寧绋掔涵顣攊versal 闂佸憡鐗曢幊鎰板极閵堝棛顩?GitHub 闂佺儵鏅滃娆掋亹閸岀偞鏅?
    // arm64 闂佸憡顨嗗ú鏍偓鍨耿閹粙濡搁妶鍥缂傚倷鐒︾换鍌炲Φ閸ャ劌绶炴慨姗嗗幗濞堝爼鏌ｉ鍡楁瀾婵犙€鍋撻梺鎸庣☉婵傛梻绱炵€ｎ剛鐭撴い鏍ㄧ懅閻?13 MB 闂傚倸瀚粔鎾春瀹€鈧惀?5 MB闂?
    // AGP 婵炴垶鎸哥粔鎾储閹寸姵濯?ABI 闂佺懓鍢插Λ妤呭垂鎼淬垻鈻?AAB 闂佸憡鑹鹃張顒€顪冮崒鐐插嚑闁告洦鍋嗙槐锕傛煥濞戞ɑ婀板褍绉电粋鎺楀Ψ閿曗偓閳?AAB 闂佸搫鍟晶搴ㄥ极?-PzippestBundle 闂佺绻楀▍鏇犳暜閳ь剟鏌熷畡鐗堫棞闁搞劌閰ｆ俊?
    splits {
        abi {
            isEnable = !emulatorBuild &&
                !providers.gradleProperty("zippestBundle").isPresent
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeUi = "1.10.4"

    implementation("androidx.core:core-ktx:1.19.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.documentfile:documentfile:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("androidx.compose.ui:ui:$composeUi")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeUi")
    implementation("androidx.compose.foundation:foundation:$composeUi")
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.apache.commons:commons-compress:1.27.1")
    implementation("org.brotli:dec:0.1.2")
    implementation("net.java.dev.jna:jna:5.17.0@aar")
    implementation("org.tukaani:xz:1.10")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("com.github.junrar:junrar:8.1.2")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("me.zhanghai.android.libarchive:library:1.1.4")

    debugImplementation("androidx.compose.ui:ui-tooling:$composeUi")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
