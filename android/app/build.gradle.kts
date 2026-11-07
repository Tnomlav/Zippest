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
// 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偞鐗犻、鏇㈡晜閽樺缃曟繝鐢靛Т閿曘倝鎮ч崱娆戠焼闁割偆鍠撶粻楣冩煕閳╁喚娈滈柣娑欑矒閺屻倝寮堕幐搴′淮闂佸搫澶囬崜婵嗩嚗閸曨厸鍋撻敐搴濈凹妞ゆ梹娲熷铏圭磼濡儵鎷婚梺鍛婎焼閸ャ劌鈧寧绻涘顔荤凹闁绘挻娲熼弻鐔煎箥閾忣偅鐝旀繝寰枫倖纭堕柟鍙夋倐閹囧醇閵忕姴缁╂繝娈垮枛閿曘儱顪冮挊澹╂盯宕橀鑲╁幐闂佸憡鍔﹂埀顒夊灲缂嶄礁顫忛搹鍦＜婵☆垵顕х喊宥咁渻閵堝繒鐣辩€规洦鍓欏畵鍕⒑瑜版帒浜伴柛鎿勭畵閹?6_64闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏃堟暜閸嬫挾绮☉妯诲櫧闁活厽鐟╅弻鐔封枎闄囬褍煤椤撱垹绠圭憸鐗堝俯閺佸啴鏌曢崼婵嗩劉缂佷緡鍋婂缁樻媴缁嬫妫岄梺绋款儎缁舵艾鐣疯ぐ鎺撴優闁稿繐顦禍鐐叏濡厧甯跺褎鎸抽弻锛勪沪閸撗勫垱濡ょ姷鍋涘ú顓炵暦濡ゅ懎浼犻柕澶堝劚濮ｅ牓姊婚崒娆戭槮闁圭⒈鍋勮灋婵°倕鍟畷鍙夋叏濡炶浜鹃悗?PzippestEmulator闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏃堟暜閸嬫挾绮☉妯诲櫧闁活厽鐟╅弻鐔告綇妤ｅ啯顎嶉梺鎼炲€栭崝鏍Φ閸曨垰鍐€闁靛ě鍛獥濠电偛鐡ㄧ划灞斤耿闁秴绠熼柟闂寸缁狅綁鏌ㄩ弴妤€浜炬俊妤€鎳樺娲捶椤撶儐鏆┑鐘灪椤洤鈽夐悽绋跨劦妞ゆ帒瀚埛鎴犵磽娴ｅ顏呮叏閸ヮ剚鐓熼幒鎶藉礉閹存繄鏆﹂柡鍥ュ灩鍞梺鍐叉惈閸婂宕㈡禒瀣拺缂備焦鈼ら鍕庣喐绻濋崶褏锛熼棅顐㈡处缁嬫帡鎮￠弴銏＄厽闁哄啫鍋嗛悞楣冩煃缂佹ɑ绀嬮柡宀€鍠栭、娑樷枎閹寸姷宕叉俊鐐€戦崹娲儎椤栫偛绠栨繛鍡樻尭缁犵敻鏌熼悜妯虹仸妞ゆ挸顭峰濠氬磼濞嗘埈妲梺瑙勭ゴ閳ь剝绉ú顏呮櫇闁稿本姘ㄩ敍娑㈡⒑缁洖澧查柣鐕傜畵閿濈偤寮撮姀锛勫弳闂佸搫娲ㄩ崑娑㈠焵椤掆偓缂嶅﹪骞?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 91
        versionName = "1.11.11"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈缁犱即鏌ゆ慨鎰偓鏇㈠几閸懇鍋撻獮鍨姎婵炶绠戦悾鐑藉蓟閵夛妇鍘遍梺鏂ユ櫅閸熶即骞婇崨瀛樼厽妞ゆ挾鍎愬Ο鈧梺鍝勬湰閻╊垶宕洪崟顖氱妞ゅ繋绀侀～宀勬⒒娴ｅ憡鎯堥柟宄邦儔瀹曟粓鎮㈤悡搴ゆ憰濠电偞鍨崹娲疾濠婂牊鐓熼柨婵嗘噽閻忛亶鏌ㄥ☉娆戞綕 / libarchive闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏃堟暜閸嬫挾绮☉妯诲櫧闁活厽鐟╅弻鐔封枎闄囬褍煤椤擃潿鈧礁鈽夐姀鈥斥偓鐑芥煛婢跺鐏ｇ紓鍌涙尰娣囧﹪鎮欓鍕ㄥ亾閺嶎厼绀夌憸鏂款潖娴犲绀嬫い鏍ㄦ皑閿?mips闂傚倸鍊搁崐鎼佸磹妞嬪孩顐芥慨姗嗗墻閻掔晫鎲稿鍫罕闂備礁鎼崯顐﹀磹閼恒儲鍙?6 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弬鍨挃闁活厽鐟╅弻鐔封枎闄囬褍煤椤撱垹绠栧ù鐘差儏缁犲綊鏌ｉ幇銊︽珔濞存粓绠栭弻娑樼暆閳ь剟宕戝☉銏″亗婵炴垯鍨洪悡鏇㈡煙閺夊灝鎮佺紒銊ょ矙閺屻劌鈽夊▎鎴犵杽濠殿喖锕ュ钘夌暦椤愶箑绀嬫い鎾寸⊕閻庨亶姊绘担鍛婂暈闁规瓕宕电划娆撳箣濠靛棛銈┑锛勫亼閸婃牠鎮у鍫濈；婵炴垶姘ㄦ稉宥囨喐閻楀牆绗氶柛濠傜仛缁绘盯骞嬮悜鍡曠礂闂佺懓顕慨鎶芥倿婵犲啨浜滈柟鎵虫櫅閳ь剚顨堟竟鏇熺附閸涘﹦鍘搁梺鍛婂姧缁茶姤绂嶉幆褉鏀介柣鎰絻閹垿鏌ｉ悢鍙夋珚闁靛棔绀侀～婵堟崉娴ｆ洏鍔庨幉鍛婂緞閹邦剛锛涢梺鐟板⒔缁垶寮查弻銉︾厱閻忕偛澧介埊鏇㈡煟濠靛洨澧垫慨濠冩そ閹筹繝濡堕崨顔锯偓鍓х磽娴ｇ瓔鍤欓柣妤佹尭椤曪絾绻濆顓炰簻闂佺粯鎸告鎼佹倵椤撶儐娓婚柕鍫濇婵呯磼閼碱剚娅曞瑙勬礋閸╋繝宕橀鍡闯濠电偠鎻紞鈧繛鍜冪悼閺侇喖鈽夐姀锛勫弳闂佸搫娲﹂敋闁诲繆鏅犻弻锝夋晲閸パ冨箣闂佽鍠掗弲婵嬪箯閻樿绠甸柟鐑樻⒒瀹曞搫鈹戦敍鍕杭闁稿﹥鐗犲畷婵單旈崨顓犵崶闁瑰吋鐣崹濠氥€?
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

    // 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愯弓鐢绘俊鐐€栭悧妤冪矙閹炬眹鈧懘宕ｆ径宀€顔曢梺绯曞墲閿氶柣蹇ュ閳ь剝顫夊ú鏍ь嚕閸洖绠為柕濞垮剻閻旂厧鍨傛い鏃€鍎崇敮鍧楁⒒?ARM 闂傚倸鍊搁崐宄懊归崶褏鏆﹂柛顭戝亝閸欏繘鏌熼幆鏉啃撻柛濠傛健閺屻劑寮撮悙娴嬪亾閸濄儱顥氶柛褎顨嗛悡娆撴偣閸濆嫭鎯堟い銉у仱閺屾盯濡烽鐓庘拻闂佽桨绀佸Λ婵嬪蓟濞戙垹鍗抽柕濞垮劚椤晠姊洪崨濠冨鞍濠电偛锕ら～蹇涙惞閸︻厾锛滃┑鈽嗗灥瀹曠敻宕ｉ崱妯肩瘈闁靛繈鍨洪崵鈧梺鎸庢处娴滄粓鎮惧畡鎵虫斀閻庯綆浜為崝锕€顪冮妶鍡楃瑨閻庢凹鍙冮崺娑㈠箣閻愮數顔曢梺鑲┾拡閸撴稓鏁☉妯锋斀闁绘劕寮堕崰姗€鏌＄仦鍓ф创鐎殿噮鍓涢幑鍕Ω瑜嶆慨閬嶆煟鎼淬値娼愭繛鍙夌墵楠炴牠顢曢敂钘変患濠电娀娼ч鍛矆鐎ｎ偁浜滈柟鍝勬娴滈箖姊洪崨濠冩儎闁告瑥鍟～蹇撁洪鍕獩婵犵數濮撮崯顐λ囬埡鍌滅閻庢稒顭囬悳鑽ょ磽瀹ュ嫮绐旂€规洖鎼埥澶愬閻樿鏁归梻渚€娼х换鍫ュ垂婵犳碍鏅?ABI闂傚倸鍊搁崐鎼佸磹閻戣姤鍊块柨鏃堟暜閸嬫挾绮☉妯诲櫧闁活厽鐟╅弻鐔兼倻濡儵鎷归梺绋跨箲缁秹濡甸崟顖涙櫜闁糕剝蓱閺嗙€rsal 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愯弓缃曢梻浣告惈濞层垽宕归崷顓犱笉闁绘绮悡娆撴煟濡も偓閻楀﹦娆㈤弻銉︾厸閻庯絺鏅濈粣鏃堟煛鐏炵偓绀嬬€规洜鍘ч埞鎴﹀炊閼哥楠忓┑?GitHub 闂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ礀绾惧潡鏌ｉ姀銏╃劸闁汇倗鍋撶换婵囩節閸屾碍鐏撻梺缁樺笩婵倗鎹㈠☉銏犲耿婵☆垵娅ｉ缁樼箾鐎涙鐭婄紓宥咃躬瀵鈽夊Ο鍏兼畷闂侀€炲苯澧寸€规洘鍨块幃娆撳传閸曨厽鐓?
    // arm64 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐椤旂懓浜鹃柛鎰靛枛楠炪垺淇婇悙瀛樼婵＄偠妫勯锝嗙鐎ｅ灚鏅濋梺缁樻濞撹绔熼弴銏♀拺闁告捁灏欓崢娑㈡煕鐎ｎ偅宕岄柟铏崌瀹曠螖娴ｅ搫骞嶆俊鐐€栧Λ浣哥暦閻㈠憡鍎楁繛鍡楃箚閺€浠嬫煃閳轰礁鏆為柛濠冨姇閳规垿鍨惧畷鍥х厽閻庤娲栭悥濂搞€侀弮鍫濈闁割煈鍋掑Λ銊х磽閸屾艾鈧悂宕愰幖浣哥９闁归棿绀佺壕褰掓煟閹达絽袚闁稿﹤娼￠弻銊╁籍閸屾矮澹曢悗瑙勬礃閻擄繝寮诲☉妯兼殕闁逞屽墴瀹曟垵鈽夊▎妯绘暞闂傚倸鍊烽悞锕傚箖閸洖纾挎い鏍仜缁€澶屸偓鍏夊亾闁逞屽墮閳诲酣濮€閵堝棗鈧兘鎮楀☉娅亜顕ｉ崹顔规斀妞ゆ梻鐡斿▓鏃€淇婇锝庢疁闁绘侗鍠栬灃濞达綀娅ｉ鏇㈡⒑閸撴彃浜濈紒璇插瀹曟椽鏁愭径瀣幐閻庡厜鍋撻悗锝庡墰琚︽俊鐐€戦崹鍝勎涢崘銊ф殾闁告鍊ｉ弮鈧幏鍛偘閳ュ厖澹曢梺璇″瀻閳ь剟寮ㄦ禒瀣厽婵☆垵宕甸幉鍧楁煕鐎ｎ偅宕岄柡灞剧〒閳ь剨缍嗛崑鍛暦鐏炵偓鍙忓┑鐘插暞閵囨繃顨ラ悙鏉戝闁诡垱妫冮弫鎰板磼濞戣鲸缍屽┑鐘垫暩婵炩偓婵炰匠鍥х９闁荤喖鍋婂鏍煠婵劕鈧牠锝為弴銏＄厽闁归偊鍣弳顖炴煕鐎ｎ偅宕岄柡浣瑰姈閹棃鍨鹃幇浣归獎闂傚倷鐒﹀鍧楀储娴犲绠板┑鐘宠壘妗呴梺鍛婃处閸ㄤ即宕橀埀顒勬⒑闂堟丹娑㈠礋椤愶絿褰梻?13 MB 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌涘┑鍕姉闁稿鎸搁～婵嬫偂鎼粹槅娼剧紓鍌欑贰閸犳螞閸愵喚宓佹慨妞诲亾妞ゃ垺鐟╁畷妤呭礂婢惰宀搁弻锝嗘償閳ュ啿杈呴梺绋款儐閹瑰洭寮诲鍫闂佸憡鎸鹃崰鏍嵁閸℃稑鐒?5 MB闂?
    // AGP 濠电姷鏁告慨鐑藉极閹间礁纾婚柣鎰惈閸ㄥ倿鏌涢锝嗙缂佺姳鍗抽弻娑樷攽閸曨偄濮㈤梺娲诲幗閹瑰洭寮婚悢铏圭＜闁靛繒濮甸悘鍫ユ⒑缁嬭儻顫﹂柛鏃€鍨垮濠氭偄鐞涒€充壕闁汇垺顔栭悞楣冨冀閳╁啰绡€?ABI 闂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ礀缁犵娀鏌熼幑鎰靛殭閹兼潙锕弻鐔虹磼濡搫鐎┑鐐村灦椤倿寮崼婵嗙獩濡炪倖鎸鹃崑鐔煎焵椤掑寮柡宀€鍠栧畷妤呮嚃閳哄倹顔冮梻浣规偠閸斿矂鈥﹂悜钘夋槬?AAB 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偛顦甸弫鎾绘偐閸愬弶鐤勫┑掳鍊х徊浠嬪疮椤愩倐鍋撳顒夋Ч闁靛洤瀚伴獮鎺楀幢濡炴儳顥氬┑锛勫亼閸娿倖绂嶅鍫濈柈闁哄鍨归弳锕傛煙閻戞﹩娈曢柛濠勫厴閺岀喓绱掑Ο铏圭懆濠电偛鐗勯崐婵嗩潖缂佹ɑ濯撮柛娑橈工閺嗗牆鈹戦悙棰濆殝缂佺姵鎸搁悾鐑藉箣閿曗偓楠炪垺绻涢崱妯哄Е婵炶偐鍠栧娲箰鎼淬垻顦ラ梺绋匡工缂嶅﹪骞冮敓鐘参ㄩ柨鏂垮⒔椤旀洟姊洪悷閭﹀殶闁稿﹥鐗犻獮瀣攽閹惧厜鍋撻崸妤佺厸濠㈣泛顑愰崕銉╂煟閺傛寧顥犻柟鑼焾椤撳ジ宕堕埡鍐帬闂備焦鐪归崹濠氬极閹间焦鍎楀鑸靛姈閻撴瑩鏌ｉ幋鐑囦緵婵炲牊妫冨畷顒勵敍閻愮补鎷洪梺鍦焾鐎涒晝澹曢悽鍛婄厱閻庯綆鍋呭畷宀勬煛?AAB 闂傚倸鍊搁崐鎼佸磹妞嬪海鐭嗗〒姘ｅ亾妤犵偞鐗犻、鏇㈡晝閳ь剟鎮块濮愪簻闁规澘澧庣粙缁樼箾閸涱垰鈻堟鐐寸墪鑿愭い鎺嗗亾濠碘€炽偢閺?-PzippestBundle 闂傚倸鍊搁崐鎼佸磹閻戣姤鍤勯柛顐ｆ磵閳ь剨绠撳畷濂稿閳ュ啿绨ユ繝寰锋澘鈧劙宕戦幘缁樼厓閻犲洩灏欐晥閻庢鍠楅幐鎶藉箖閳哄懏鎯炴い鎰剁秵閸炲爼姊婚崒娆戠獢闁逞屽墰閸嬫盯鎳熼娑欐珷闁圭虎鍠楅悡娑氣偓鍏夊亾闁逞屽墴瀹曠増鎯旈妸锕€浠奸梺璺ㄥ枔婵敻宕戠€ｎ喗鐓曟い鎰靛亜娴滄繃銇勯妷顖滅М婵﹦绮幏鍛喆閸曨偂鍝楅梻浣告啞閺屻劑鎯岄崒鐐茶摕濠电姴娲﹂弲鏌ユ煕閳╁啯绀堢紒?
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
