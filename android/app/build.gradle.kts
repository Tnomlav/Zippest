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
// 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敂钘変罕濠电姴锕ら悧鍡欑矆閸喓绠鹃柛鈩冾殜閻涙粓鏌ら弶鎸庡仴闁哄备鍓濆鍕偓锝庝簼椤旀洟姊虹紒妯烩拻闁告鍥ㄥ€挎繛宸簼閻撴洟鏌熼幍铏珔濠德ゆ硶閹叉悂鎮ч崼銏犲绩濠殿喖锕ュ钘壩涢崘顔肩厸闁告劦鈧垽缍佸铏圭磼濡纰嶅銈忕畱瀹曨剙宓勯梺褰掑亰閸擄箓鎮?6_64闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熷▎陇顕уú顓㈠箹瑜版帩鏁冮柕鍫濆缁侇偊姊绘担绋款棌闁稿绶氬畷褰掓惞閸忓浜炬慨妯煎帶婢ф挳鏌＄仦鍓ф创妤犵偛娲畷妤呭传閵夈儱姣堥梻鍌欑閹碱偄螞濡ゅ啯宕叉慨妞诲亾鐎?PzippestEmulator闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熸潏楣冩闁搞倖鍔栭妵鍕冀閵娧呯窗婵炲瓨绮屽﹢閬嶅箟閹间礁绠ｉ柨鏇楀亾濡楀懘姊洪崷顓烆暭婵犮垺顭囧☉鐢稿焵椤掑嫭鈷戠紓浣姑慨鍥煟鎺抽崝鎴濈暦閺囥垹唯闁冲搫鍊婚崢浠嬫⒑缂佹◤顏勎熸繝鍥х？闊洦绋掗悡鏇㈡煟閺冨偆鐒鹃柍缁樻礋閺岀喖顢涘▎鎴犵崲濡炪們鍨洪惄顖炲箖濞嗘挸绠甸柟鐑樺灦椤撳姊婚崒娆戭槮闁规祴鈧秮娲晝閸屾氨锛涢梺绯曞墲閻燂箓锝為弴銏＄叆闁哄洨鍋涢埀顒€缍婇幃?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 90
        versionName = "1.11.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 婵犵數濮烽弫鎼佸磻閻愬搫绠伴柤濮愬€曢弸鍫⑩偓骞垮劚濞诧箑鐣烽弻銉︾厱闁斥晛鍟伴幊鍛存煟椤撶儐妯€闁哄本鐩崺鍕礃椤忎礁顫岄梻浣告惈閹峰宕滈悢鐓庤摕婵炴垯鍨洪弲婊堟煟閿濆懐鐏遍柨娑欑潝 / libarchive闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熷▎陇顕уú顓€佸☉銏″€烽柡澶嬪灣缂傛挻淇婇悙顏勨偓鏍礉瑜斿浠嬪礋椤栨氨锛?mips闂傚倸鍊搁崐椋庢濮橆剦鐒界憸宥堢亱闂佸搫鍟崐鑺ユ叏?6 缂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏂垮⒔閻瑩鏌熷▎陇顕уú顓㈠箖娴犲绠归柣鎰ㄦ櫅娴滈箖鏌涘畝鈧崑娑㈡偂濞戙垺鐓曢柟鏉垮悁缁ㄤ粙鏌ㄥ☉娆戠疄婵﹥妞藉畷顐﹀礋椤撴稒鐎遍梻浣告啞閹歌崵绮欓幋婵嗙カ婵＄偑鍊栭悧婊堝磻濞戞氨涓嶇憸鐗堝笚閸婂灚绻涢幋鐑嗕紗闁瑰濮抽悞濠冦亜閹扳晛鈧澹曟總鍛婄厸闁告劧绲芥禍鎯р攽閻愭彃鎮戦柣鐔叉櫊閵嗕礁顫濈捄浣曘劎鎲告径鎰；闁瑰墽绮弲鏌ユ煕鐏炲墽鈯曢柣婵囩墵濮婃椽鎳￠妶鍛€剧紓浣筋嚙閻楁挸顕ｆ繝姘亜闁绘挸楠搁悗顓烆渻閵堝棗濮х紒鑼櫕婢规洟鍩￠崘顏嗭紳婵炶揪缍€濞咃絿鏁☉銏＄叆闁哄洦锚閻忊晠鏌ｉ敐鍥у幋闁诡喒鏅濋幏鐘诲箵閹烘梻宕哄┑锛勫亼閸婃牠宕濆Δ鍛獥閹兼番鍨婚々?
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

    // 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭画濡炪倖鐗楃粙鎾汇€呴崣澶岀闁糕剝锚閻忥妇鈧娲栧鍫曞箞閵娿儙鐔煎垂椤旀儳甯块梻?ARM 闂傚倸鍊峰ù鍥х暦閸偅鍙忛柟鎯板Г閸婂潡鏌ㄩ弴鐐测偓鍝ュ閸ф鐓欓悹鍝勬惈椤ョ偤鏌涢妷顔煎⒒闁轰礁妫濋弻娑㈠即閵娿儱顫╅梺鍛婃尰婵炲﹤顫忛搹鍦＜婵☆垵宕甸崣鍡樼節閵忋垺鍤€闁挎洦浜滈悾宄扳攽鐎ｎ亞鍔﹀銈嗗笒鐎氼參鍩涢幋鐐电闁肩⒈鍓涚敮娑樷攽閻愬弶鍠橀柡灞剧洴瀵剛鎹勯妸褍濮遍柣搴ゎ潐濞叉牠骞栭锔藉仼婵犻潧顑呯粈瀣亜閹哄棗浜鹃梺鍛婃惄閸欏啫顫忓ú顏勫窛濠电姴鍟ˇ鈺傜箾鐎涙鐜荤紓宥勭窔瀹曞搫鈽夐姀鐘诲敹闂侀潧绻堥崹濠氭晬?ABI闂傚倸鍊搁崐鐑芥倿閿旈敮鍋撶粭娑樻噽閻瑩鏌熼悜妯烩拹闁稿繑绮嶉妵鍕晲閸℃ɑ鏆瀡ersal 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐蹭罕闂佸搫娲㈤崹鍦不閻樼粯鐓欓柣妤€鐗婄欢鏌ユ煛鐎ｂ晝绐旈柡灞炬礋瀹曠厧鈹戦崶鑸碉骏婵?GitHub 闂傚倸鍊搁崐鐑芥嚄閸洖纾块柣銏㈩焾閻ょ偓绻濇繝鍌氭灓闁绘帟濮ょ换娑㈠幢濡櫣顏绘繛瀛樼矊缂嶅﹪寮诲☉妯兼殕闁逞屽墴瀹曟垿鎮欓崫鍕煣?
    // arm64 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩顔瑰亾閸愵喖骞㈡俊鐐存礃濡炶棄顕ｆ禒瀣垫晝闁绘棁娓规竟鏇㈡⒑閸撹尙鍘涢柛瀣崌閹虫捇宕稿Δ浣哄幍濡炪倖妫佸畷鐢告儗濞嗗繆鏀介柍鈺佸暞閸婃劕鈹戦垾宕囧煟鐎规洖鐖奸、鏃堝礋閸偒妫ㄧ紓鍌氬€搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弮鍌氫壕鐎规洘鐓￠弻娑樼暆閳ь剟宕戝☉娆樻敯闂傚倷鐒﹂幃鍫曞磿椤栫偛绀夌€光偓閳ь剙鈻庨姀銈嗗€烽悗娑櫭鍨攽椤旂瓔娈旀俊顐ｎ殜閻涱喖螖娴ｈ櫣顔曢梺鍓插亝缁诲嫰宕洪敐澶嬬厸鐎光偓鐎ｎ剛袦濡炪們鍨哄Λ鍐ㄧ暦閸楃倣鏃€鎷呯悰鈥充壕闁诡垎鈧弨浠嬫煟濡崵鎲块柛瀣崌閺屾稓鈧綆鍋呭畷灞炬叏婵犲啯銇濇鐐村姈閹棃鏁愰崒娑辨綌婵犵數濞€濞佳囧磹閻熼偊娼栭柤濮愬€栭～鏇㈡煟閹邦噮鏆柛瀣崌閺佹劖鎯旈垾鎰佹骄闂備焦妞块崢浠嬪箰婵犳艾桅闁告洦鍨伴崘鈧梺闈浤涢崟顐ｇ彯闂?13 MB 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柛婵勫劗閸嬫挸顫濋悡搴☆潾缂備緡鍠楀Λ鍐嵁濮椻偓椤㈡瑩宕楅崗澶规岸鏌ｆ惔鈥冲辅闁稿鎹囬弻宥堫檨闁告挾鍠栭獮鍡涘焵?5 MB闂?
    // AGP 婵犵數濮烽弫鎼佸磻閻愬搫鍨傞柛顐ｆ礀缁犱即鏌涘┑鍕姢闁活厽鎹囬弻鐔虹磼閵忕姵鐏堥梺绋胯閸旀垿寮婚悢琛″亾閻㈡鐒鹃弽鈩冪節?ABI 闂傚倸鍊搁崐鐑芥嚄閸洖绠犻柟鎹愵嚙鎼村﹪鏌熺紒妯哄瀭婵炴垶顭傞弮鍫濆窛妞ゆ挾鍋熼埀顒夊弮閺岀喖宕楅懖鈺傛闂佹悶鍔岄…鐑藉春?AAB 闂傚倸鍊搁崐椋庣矆娓氣偓楠炲鏁撻悩鍐叉疄婵°倧绲介崯顐も偓姘槹閵囧嫰骞掗崱妞惧婵＄偑鍊ゆ禍婊堝疮閺夋垹鏆﹂柟鐑橆殕閸婄兘鏌熺紒妯虹瑨婵炲牄鍊濆缁樻媴閸涘﹤鏆堝┑鐐额嚋缁犳挸鐣烽幋锕€骞㈡繛鍡樺姦濞肩喖姊洪幐搴㈢闁稿﹤缍婇幃锟犲Ψ閿斿墽顔曢梺鐟邦嚟閸婃牠骞嬪┑鎾光偓鍧楁煛婢跺鍎ラ柣鏂挎閹茬顓奸崶鈺冪厠闂佹眹鍨婚弫鎼佹儗婢舵劖鐓欓柣鎴烇供濞堟棃宕鐐粹拺闁圭瀛╃壕鐢告煕鐎ｎ偅宕岄柡?AAB 闂傚倸鍊搁崐椋庣矆娓氣偓楠炴牠顢曢敃鈧悿顔姐亜閹板墎绋绘繛鍛█楠炴牕菐椤掆偓婵″ジ鏌?-PzippestBundle 闂傚倸鍊搁崐鐑芥嚄閸洍鈧箓宕奸姀鈥冲簥濠德板€愰崑鎾绘煃鐠囪尙效鐎殿喗鎸抽幃鈺呮惞椤愶綆鍞堕梻鍌欑窔閳ь剛鍋涢懟顖涙櫠閹绢喗鐓涚€光偓閳ь剟宕版惔銊﹀仼闁跨喓濮甸崑瀣煕椤愵偄浜濇い銉秮濮婄粯鎷呯憴鍕哗闂佸憡鏌ㄩ惌鍌炲蓟婵犲洦鏅查柛鈩冩礈缁?
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
