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
// 闂傚倸鍊风粈渚€骞栭锔藉亱婵犲﹤鐗嗙粈鍫熺箾閸℃鐛滈柤鏉挎健閺屸剝寰勭€ｎ亝顔曢梺缁樻⒒閸樠囨倿濞差亝鐓曢柟鎵虫櫅婵¤法鎲搁悧鍫㈠弨婵﹥妞藉Λ鍐煛閸愵€綁姊虹紒妯诲碍妞わ箑宕嵄闁归偊鍓﹂悗?6_64闂傚倸鍊烽悞锔锯偓绗涘懐鐭欓柟娆¤娲幖褰掝敃閵堝孩绁梻浣稿閸嬩線宕归搹鍏夊亾濮樼厧澧撮柡灞剧洴楠炲洭宕楅崫銉ュ毈闂備礁鎼Λ妤冩崲濮椻偓瀵?PzippestEmulator闂傚倸鍊烽悞锔锯偓绗涘懐鐭欓柟杈鹃檮閸ゆ劖銇勯弽銊х細濞存粌婀遍幉鎼佸箣閿曗偓妗呴梺鍦濠㈡娑甸埀顒勬⒑缂佹ê濮囬柣掳鍔戝畷鏇㈠Ψ閳哄倻鍘介梺缁樏Ο濠囧磿韫囨稒鐓曢柣鏃傤焾閳绘洟鏌熼娆戠獢妞ゃ垺鐩幃娆撳箵閹烘垶顓婚梻鍌欑閹测€趁洪敃鍌氱；闁糕剝鐟﹂～鏇㈡煥閺囩偛鈧綊鎮?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 89
        versionName = "1.11.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 濠电姷鏁搁崑鐐哄箰閼姐倕鏋堢€广儱娲﹀畷鏌ユ煕閳╁啰鎳呴柣顓烆樀閺屾盯鍩勯崘顏佸闂佸搫鎷嬮崜鐔煎蓟濞戙垺鏅滈柣锝呯灱閿涙睔 / libarchive闂傚倸鍊烽悞锔锯偓绗涘懐鐭欓柟娆¤娲、娑㈡倷閺夋垳缂撴俊鐐€栭崝褔姊介崟顖氱？?mips闂傚倸鍊风欢姘焽瑜嶈灋闁哄啫鍊芥慨?6 缂傚倸鍊搁崐鐑芥倿閿斿墽鐭欓柟娆¤娲幃浠嬪箹閻愨晛浜鹃柛宀€鍋涢悡娑㈡煕閹板吀绨介柨娑欑箞濮婃椽宕崟顓涙瀱闂佸憡鎸荤粙鎴濆祫濡炪倖鐗滈崑娑氱不瑜版帗鍊垫繛鎴烆伆閹寸姳鐒婃い鎰╁€楃壕濂告煛閸愶絽浜惧┑鐐插悑閻熲晠銆佸璺何ㄧ憸澶愬磻閹剧粯鏅查柛灞剧⊕閻濇牠姊洪懡銈呮瀾缂佽鐗撳濠氭偄閻撳骸鐎銈嗗姧缁茬晫澹曢埡鍐＝濞达綀娅ｇ敮娑㈡煥閺囨ê鐏╅柣锝囧厴閹晝鎷犻幓鎺旂崺婵＄偑鍊栭崝妤呭窗鎼淬垻顩?
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

    // 闂傚倸鍊风粈渚€骞夐敓鐘冲仭妞ゆ牗绋撻々鍙夌箾閸℃ê鐏︾€规洖寮堕幈銊ノ熼崹顔惧帿闂?ARM 闂傚倷娴囧畷鍨叏閹惰姤鍊块柨鏇炲€哥壕鍧楁煙鐠哄搫顥為柛銉墻閺佸棝鏌涢弴銊ュ闁告挻濞婂铏圭磼濡崵鍙嗘繝銏㈡嚀閿曨亜鐣峰┑瀣劦妞ゆ帒瀚埛鎴炵箾閼碱剛甯涘┑鐐叉喘閺屾盯寮捄銊у姱閻庤娲栭幖顐︽偩濠靛绀嬫い鎺嗗亾闁告搩鍙冨娲川婵犲啫顦╂繛瀛樼玻缂嶄線宕哄☉銏犻唶闁靛繈鍨婚敍?ABI闂傚倸鍊烽悞锔锯偓绗涘懐鐭欓柟鐑樻⒐閸忔粍銇勯敐鍡樻暞versal 闂傚倸鍊风粈渚€骞夐敓鐘冲亱闁哄洢鍨圭粻鐘绘煙閻楀牊绶查柡瀣╃窔閺屾洟宕煎┑鍥舵￥濡?GitHub 闂傚倸鍊烽懗鍫曞磿閻㈢鐤炬繝濠傚枤閻掕姤绻涢崱妯虹濞存粌缍婇弻娑樼暆閳ь剟宕戦悙鍝勭柧?
    // arm64 闂傚倸鍊风粈渚€骞夐敓鐘偓鍐幢濡炴洘妞藉浠嬵敃閻旇渹澹曢梺鍓茬厛閸嬫捇鎳撻崸妤佺厵妞ゆ棁宕甸惌娆忊攽閳╁啯鍊愬┑鈥崇埣瀹曞爼顢旈崟鍨棨缂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏃傚亾瀹曟煡鏌涘畝鈧崑娑欘攰闂備焦鎮堕崕顖炲礉瀹€鈧▎銏ゆ倷鐎涙ê寮垮┑顔筋殔濡鐛Δ浣虹闁割偅绻勯崺锝夋煛瀹€瀣М妞ゃ垺妫冨畷鍗炍旀担琛″亾閹€鏀介柣妯荤憿閸嬫捇鏌涚€ｎ偅宕屾慨濠冩そ楠炴劖鎯旈敐鍌涱潔濠电娀娼ч崐鐟邦潖閼姐倖顫曢柣鎰暯閸嬫捇鏁愭惔鈥愁潾闂佹椿鍘介幐濠氬Φ閸曨垰鍐€闁靛ě鍕珮闂?13 MB 闂傚倸鍊搁崐鎼佸磹閸濄儮鍋撳鐓庡缂侇喗妫冮獮姗€顢欓崗鍏夹氶柣搴″帨閸嬫捇鏌嶈閸撶喖骞嗛埀?5 MB闂?
    // AGP 濠电姷鏁搁崑鐐哄垂閸洖绠伴柛婵勫劤閻捇鏌熺紒銏犳灈闁稿被鍔戦弻鐔衡偓鐢殿焾鏍℃繝?ABI 闂傚倸鍊烽懗鍫曞箠閹捐搴婇柟缁樺垂濞戞鏃堝川椤撶偟鈧參鏌熼崗鑲╂殬闁搞劌顭烽崺?AAB 闂傚倸鍊风粈渚€骞夐敓鐘冲殞濡わ絽鍟€氬銇勯幒鍡椾壕濡炪値浜滈崯鏉戠暦閹烘鍊烽柟缁樺笒濞堛倝姊绘担鍛婂暈婵炶绠撳畷鎴﹀幢濞嗘劦娼熼梺鎸庢礀閸婂綊鎮￠妷锔剧闁瑰鍊栭幋婵撹€块柡澶嬵儥閻斿棛鎲稿鍥╃煋闁汇垻鏁搁惌澶愭煙閻戞﹩娈旈崬顖炴⒑閹稿孩纾甸柛瀣崌閺?AAB 闂傚倸鍊风粈渚€骞栭锕€鐤い鎰剁稻濞呯娀骞栧ǎ顒€濡奸柡?-PzippestBundle 闂傚倸鍊烽懗鍫曗€﹂崼銏″床婵°倐鍋撻柍璇茬Ч瀵挳鎮╅搹顐ｎ唶闂備線鈧偛鑻晶鎾煛瀹€鈧崰搴ㄦ偩閿熺姵鍋嬮柛顐亝椤ワ綁姊绘担瑙勫仩闁告柨鐭傞弻濠囨晲閸℃洜绠?
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
