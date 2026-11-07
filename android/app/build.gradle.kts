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
// 闂傚倷绀侀幖顐︽偋濠婂牆绀堟繛鍡楅獜閼板潡鏌℃径瀣闁绘梻鍘ч悞娲煕閹扳晛濡跨憸鐗堢叀濮婃椽妫冮埡鍐ｉ梺缁樻尭椤﹀崬危閹邦剦鐎?6_64闂傚倷鐒︾€笛呯矙閹次诲洭鎼归銈庢祫闂佸壊鍋侀崹铏光偓姘煎墴閺屾盯骞囬崗鍝ュ嚬闂佸搫妫楃换姗€寮?PzippestEmulator闂傚倷鐒︾€笛呯矙閹达附鍤愭い鏍ㄧ缚娴滃湱鎲搁幋锕€桅闁圭増婢樼涵鈧梺缁樺姇閻°劑宕曢妷鈺傜厽闁绘ê妯婇崕蹇涙煕閻旂鈻曢柟顖欑窔椤㈡盯鎮欓幓鎺戞闂備礁鎲″ú锕傚磻閸℃瑦顫曢柨鏇炲€归悡?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 88
        versionName = "1.11.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 婵犵數鍋為幐鑽ゅ枈瀹ュ洦宕查柛鈩冪懅閻鏌涢埄鍐姇闁哄拋鍓熼弻娑㈡晜閻ｅ瞼锛汚 / libarchive闂傚倷鐒︾€笛呯矙閹次诲洭顢涢悙鏉戜缓濡炪倖鍔ч梽鍕磿?mips闂傚倷绶氬褍螞閺冨倽濮?6 缂傚倸鍊烽悞锔剧矙閹次诲洭鎮介幖鐐╁亾閸岀偛鐓涢柛鎰典簽閿涙繈姊洪崫鍕枆闁告挻绋戝嵄妞ゆ牜鍋涚粻褰掓倵濞戞鎴犱焊椤愩倗纾奸柡鍐ｅ亾婵炲吋鐟╅、姘跺Ψ瑜夐崑鎾绘晲閸屾稒鐝栭梺鑽ゅ枑缁诲牓寮婚悢鐓庡瀭妞ゆ劧绲界壕鈺冪磽娴ｈ櫣甯涢柨鏇樺灩閻ｇ兘鎮╃拠鎻掔獩濡炪倖鍔楅崰搴㈢?
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

    // 闂傚倷绀侀幉锟犳偡椤栨稓顩叉繛鍡樺灦瀹曞弶鎱ㄥΟ鍨厫闁?ARM 闂備浇宕垫慨鎶芥倿閿曞倸纾块柟璺哄閸ヮ剦鏁嗛柛鏇ㄥ墮閸撴娊姊虹紒妯荤叆濠㈢懓锕畷婵嬪焵椤掑嫭鈷戞繛鑼帛婵炲洭鏌涢弬璺ㄧ劯鐎规洖鎼悾婵嬪礋椤掆偓閸擃參姊洪崨濠冨濞存粣缍侀崺娑㈠醇閵忋垻锛?ABI闂傚倷鐒︾€笛呯矙閹烘梹鍏滄い锝嗘敯versal 闂傚倷绀侀幉锟犳偋閺囥垹绠犻柟鐗堟緲閺嬩線鏌曢崼婵囶棤妞?GitHub 闂傚倷鑳堕崕鐢稿疾濠婂喚鐒芥繛鍡樺竾娴滃綊鏌涘畝鈧崑鐐哄疾?
    // arm64 闂傚倷绀侀幉锟犮€冮崱妞曟椽寮介鐔蜂壕闁割煈鍋撻懓鍧楁煙椤旇崵鐭欏┑鈩冩倐婵″爼宕堕鍕垫缂傚倸鍊搁崐鐑芥倿閿旂偓宕查柛宀€鍋涙闂佹悶鍎崝宀€娆㈤悙瀛樺弿婵妫楅獮妤佺箾閸繄鍩ｉ柡宀嬬秮椤㈡棃宕卞Δ浣衡偓鎯р攽閻樻瑢鍋撻柛瀣崌濮婃椽骞愭惔锝傛婵犻潧鍊瑰鑽ゆ閻愮鍋撻敐搴″闁活厽鎸婚妵鍕冀閵娧勫櫘闂?13 MB 闂傚倸鍊搁崐鍝モ偓姘煎墰缁棃骞橀鍏兼К閻庡厜鍋撻柍褜鍓熼幆鈧?5 MB闂?
    // AGP 婵犵數鍋為崹鍫曞箰閸濄儳鐭撻柟缁㈠枛閸屻劑鏌熺€电校濠?ABI 闂傚倷鑳堕幊鎾诲床閹绘崹娑樜旈崨顓炵€柟鍏肩暘閸ㄥ鍩?AAB 闂傚倷绀侀幉锟犳嚌妤ｅ啫瀚夋い鎺嗗亾妞ゎ亜鍟村畷鎺楁倷閹绘帒娈ら梻浣告啞濞诧箓宕戦崱娆愵潟闁挎洖鍊归悡銉︾箾閹寸倖鎴濓耿閺夋鐔嗙憸宥囩矓閻㈢數鐭夐柟鐑橆殔鍞梺鎸庢磵閸嬫捇鏌?AAB 闂傚倷绀侀幖顐﹀疮椤愶絾娅犻幖娣妼閺?-PzippestBundle 闂傚倷鑳堕…鍫㈡崲濡も偓閳诲秹寮撮悩铏闂侀€炲苯澧撮柡宀€鍠庨悾锟犳偋閸偅顥ｉ梻浣规偠閸斿矂鏌婇敐鍡曠箚?
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
