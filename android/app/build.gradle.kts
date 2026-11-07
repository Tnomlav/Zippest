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
// 闁哄牜鍓欏﹢鏉懳熼埄鍐ㄧ彲闁革綆鐓夌槐妾?6_64闁挎稑顦虫禒鍫㈡嫬閸愵亝鏆忛柨?PzippestEmulator闁挎稑鑻ぐ褑銇愰崡鐐存儥閻犲鍟抽惁顖炲几閸曨偆绱﹂柕?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 85
        versionName = "1.11.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 濞撴碍绻嗙粋鍡涙晬閸︽紑A / libarchive闁挎稑顦悽顐︽儍?mips闁靛棔绨?6 缂佹稑顦甸。鈺冪磽閺嶎剛妲幖瀛樻尫缁旀潙顕ョ€ｂ晝鐟濋柟鍨崄缁绘﹢宕犻崨顐熷亾?
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

    // 闁告瑯浜欑换姘舵偩?ARM 閻犱焦鍎抽ˇ顒勬儑閻斿壊鍔€闁活潿鍔岄崺宀勬儍閸曨亣鈷堢紒?ABI闁挎稒纭iversal 闁告牕鎳愰弫銈嗙?GitHub 闁烩晜娼欒ぐ鍌炴晬?
    // arm64 闁告娲栫€垫﹢鎮介妸銈囪壘缂備焦绻傞妵鍥ㄥ緞濮橆厽娈堕柣顏嗗枑濠р偓闁挎稑濂旂紞瀣矓椤栨瑧鐭?13 MB 闂傚嫬绉撮崺宀€鐥?5 MB闁?
    // AGP 濞戞挸绉撮崢鎴犳媼?ABI 闁瑰嘲妫楅崹搴㈢▔?AAB 闁告艾鏈鍌炲几閸曨偆绱﹂柨娑樻湰婢у秵绂掗妷锕€鈪?AAB 闁哄啫澧庨弫?-PzippestBundle 闁稿繗娅曠敮鈧柟宄版閸ㄥ酣濡?
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
