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
// 闂備礁鎼悧婊堝礈濞嗗骏鑰块柡澶嬪殾閻旂厧鐒洪柛鎰╁妿瑜版煡姊洪棃鈺冪У闁绘挸顦卞Σ鎰瀶?6_64闂備焦瀵х粙鎴λ囬搹顐ゎ浄闁割偁鍨虹€氼剟鏌涢幇鍏哥凹闁哄棗绻橀弻?PzippestEmulator闂備焦瀵х粙鎴︽嚐椤栨縿浜圭憸鎴﹀Φ閹版澘纭€闁绘劕鐡ㄩ崕銉╂煟閻樺樊鍎忛柛鐔稿▕閹線顢涢悙鎻掑殤闂佸憡娲﹂崑鍡欐閿曞倹鐓?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 87
        versionName = "1.11.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 濠电偞鎸荤喊宥囨崲閸℃瑧鐭夐柛鈩冪⊕閺咁剟鏌涢敂鐣岀；A / libarchive闂備焦瀵х粙鎴λ囬鐐村仺妞ゆ劧闄勯崕?mips闂備線娼уΛ鏃傝姳?6 缂傚倷鐒︾粙鎴λ囬悽鎼炩偓鍌炲煛閸愵亞锛濋梺鍝勭▉閸撴稑危椤栫偛绠归悗娑櫳戠亸顐ょ磼閺冣偓濞兼瑩顢氶妷褉鍋撻敐鍌涙珖闁荤喐绻堥弻鐔煎垂椤愶絽纾╃紓浣虹帛閿曘垹鐣烽悩璇插窛妞ゆ劗鍠庢禍?
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

    // 闂備礁鎲￠悷顖涚濞嗘垶宕叉慨妯垮煐閸?ARM 闂佽崵濮抽悞锕傚磿閹跺壙鍥敆閸曨剙鍓抽梺缁樻煥婢瑰﹪宕濋埀顒勬⒑濞茬粯濞囬柛鏂跨焸瀹曞搫鐣濋崟顒€鍓梺鍛婃处娴滐綁鍩涢崼銏㈢＜?ABI闂備焦瀵х粙鎺旀兜椤ｆ攰versal 闂備礁鎲￠悧鏇㈠箠閹版澘鏋侀柕鍫濇椤?GitHub 闂備胶鍎甸弲婊冾焽濞嗘帇浜归柛宀€鍋為弲?
    // arm64 闂備礁鎲￠〃鍡椕洪弽顐熷亾閸偓鑰块柟顔荤矙婵℃悂濡堕崶顏勵棟缂傚倸鍊烽悞锔炬崲閸岀偛桅闁搞儯鍔岀欢鐐存叏濮楀棗骞楁繛鍫濈埣閺岋綁顢旈崱妤佺€惧┑鐘欌偓閸嬫捇姊洪幐搴ｂ槈濠靛倹姊荤槐鐐碘偓锝庡墰閻挻銇勯弽銊ф噮闁?13 MB 闂傚倸鍊哥€氼剛绮旈幘顔兼槬鐎光偓閳ь剟鎯€?5 MB闂?
    // AGP 濠电偞鍨堕幐鍝ョ矓閹绢喖鍌ㄩ柟瀵稿У婵?ABI 闂備胶鎳撻崲鎻捨涘Δ鍛瀭閹兼番鍨婚埢?AAB 闂備礁鎲￠懝楣冨嫉椤掆偓椤啴宕掗悙鎻掑殤闂佸憡娲﹂崑鍡欐閿曞倹鐓ユ繛鎴炆戝﹢鏉款熆瑜嶇粔鐢电矉閹烘唯闁挎洍鍋撻柍?AAB 闂備礁鎼崯顐ｆ櫠鎼淬劌鏋?-PzippestBundle 闂備胶顭堢换妤€鈻嶉弴鐘虫殰闁逞屽墴閺岀喎鐣￠悧鍫闂佹悶鍔岄柊锝嗕繆?
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
