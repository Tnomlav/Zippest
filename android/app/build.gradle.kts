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
// 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫆闁芥ê顦純鏇熺節閻㈤潧孝闁挎洏鍊濋幃褔宕卞▎鎴犵劶闂佸壊鍋嗛崰鎾剁不妤ｅ啯鐓曢柍鈺佸枤濞堟粓鏌ｅ☉娆戠煉闁哄被鍊濆鍫曞箰鎼粹€叉樊闂備礁鎼径鍥礈濠靛棭鍤楅柛鏇ㄥ幐閸嬫捇鏁愭惔婵堝嚬濡炪倖姊瑰ú鐔奉潖閾忓湱纾兼俊顖滃劦閹峰姊洪崨濠庣劶闁搞儯鍔岄埀顒佸缁绘稑顔忛鑽ゅ嚬闂佺粯鎸诲ú鐔煎蓟閻旂厧绠ラ柧蹇ｅ亝閻濇梹绻濆鏋€栫涵鍫曟煙閸欏鍊愰柟顔ㄥ洤閱囬柕蹇曞Т缂佲晜绻濆▓鍨灈闁挎洏鍎遍—鍐寠婢光晜鐩畷姗€顢欓懖鈺佸箰闂備礁鎲￠崝锕傚焵椤掑鐏茬紓宥勭椤繘鎼归崷顓狅紲濠碘槅鍨甸褏鍠婂鍜佹富闁靛牆绻掗悾杈┾偓瑙勬处閸撴瑥鐣甸崟顖涒拺鐟滅増甯掓禍浼存煕閹垮嫮鐣甸柟?6_64闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弮鍫熸殰闁稿鎸剧划顓炩槈濡娅ч梺娲诲幗閻熲晠寮婚悢灏佹瀻闂勫洭顢氳鐓ゆい鎾卞灩缁犲湱鎲搁悧鍫濅刊闁轰礁鍟撮弻鏇㈠醇濠靛棭鍔夌紓浣风贰閸嬪﹤顫忕紒妯诲缂佸顑欏Λ宀勬⒑缁嬫鍎庣紒鑸佃壘閻ｇ柉銇愰幒鎾村劒闂佺绻愰ˇ顖涚閻愵剚鍙忔俊顖滃帶鐢泛顭胯閹告娊寮婚敍鍕勃闁告挆鍕灡婵°倗濮烽崑娑樏洪鐐垫殾婵°倕鎳庢导鐘绘煏婢跺牆鍔氭慨锝呯墦濮婂宕掑▎鎴М闂佸湱鈷堥崑鍕亱濠德板€曢崯顖氱暦閸欏鍙忔俊鐐额嚙娴滈箖鎮?PzippestEmulator闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弮鍫熸殰闁稿鎸剧划顓炩槈濡娅ч梺娲诲幗閻熲晠寮婚悢鍛婄秶濡わ絽鍟宥夋⒑閹肩偛鈧牠宕濋弽顓炍﹂柛鏇ㄥ灠閸愨偓闂侀潧臎閸涱垳鐛ユ繝鐢靛仜閻°劎鍒掔仦鏂よ€块梺顒€绉寸粻鐔兼煙闂傚顦︾紒鐙呯秮閺屻劑寮村Δ鈧禍鐐繆濡も偓閹虫ê顫忓ú顏勬嵍妞ゆ挾鍎愰弳顓炩攽閻橆喖鐏い顓炴搐閳藉鎮界粙璺ㄥ姦濡炪倖甯掔€氼參鍩涢幋鐘电＝濞达絽顫栭鍛弿闁搞儺鍓氶悡鐔煎箳閹惰棄绀夐柟瀛樼箘閺嗭箓鏌￠崶銉ョ仼閸烆垶姊洪崘鍙夋儓闁稿﹤顭峰畷銏＄鐎ｎ偀鎷虹紓鍌欑劍閳笺倝顢旈崟搴ｅ枑缁绘繈宕惰閿涚喖妫呴銏″缂佸甯￠幃锟犲即閵忥紕鍘介梺鍝勫暙閸嬪棝鎮炴ィ鍐╃厓缂備焦蓱缁€瀣煛瀹€鈧崰鏍€佸☉妯锋瀻闁瑰濮峰畷鍙変繆閻愵亜鈧垿宕瑰ú顏呭剮妞ゆ牜鍋涚粻鏍ㄧ箾閸℃ɑ灏紒鐘垫暬閺岀喖鎮滃Ο铏逛桓濡炪倖鎸搁…宄邦潖婵犳艾纾兼繛鍡樺焾濡差噣姊虹憴鍕偞闁逞屽墲缁夘喖煤椤忓懏娅囬梺绋挎湰濮樸劑鏁嶅☉銏♀拺缂侇垱娲栨晶鏌ユ煟閻曞倻鐣甸柨婵堝仱瀵挳濮€閿涘嫬寮抽梻浣告惈濞层劑宕戝☉銏犵劦妞ゆ巻鍋撶紓宥咃躬楠?
val emulatorBuild = providers.gradleProperty("zippestEmulator").isPresent

android {
    namespace = "com.decompress.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.decompress.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 132
        versionName = "1.21.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｉ幇顒佹儓缂佺姳鍗抽弻銈嗘叏閹邦兘鍋撻弴銏犲嚑闁割偀鎳囬崑鎾荤嵁閸喖濮庡┑鐐额嚋缁犳垿鎮鹃悜钘夎摕闁靛濡囬崢閬嶆⒑閺傘儲娅呴柛鐔跺嵆楠炲﹪宕ㄧ€涙鍘藉銈嗘尵閸庢劕螣閳ь剟姊洪崫鍕拱闁烩晩鍨跺畷娲礋椤栨氨顦ㄥ銈呯箣缁€渚€锝炲畝鍕拻濞达絽鎲￠幆鍫ユ煙瀹勯偊鍎旂€规洘绮撻幃銏ゆ偂鎼淬倖鎲版繝鐢靛仦閸垶宕瑰ú顏勭柧婵犲﹤鐗婇悡鐔兼煥濠靛棙鍣介柣蹇涗憾閺屻劌鈽夊▎鎴炵稌 / libarchive闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弮鍫熸殰闁稿鎸剧划顓炩槈濡娅ч梺娲诲幗閻熲晠寮婚悢灏佹瀻闂勫洭顢氳鐓ゆい鎿冩娇閳ь兛绀侀埥澶愬閳ユ枼鍋撻悜鑺ョ厸濠㈣泛顑呴悘锝囩磽閸屾稒灏板ǎ鍥э躬閹瑩顢旈崟銊ヤ壕闁哄稁鍘肩粈澶屾喐閺傛娼栧ù鐘差儏缁€瀣亜閺嶃劍鐨戦柨?mips闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣椤愯姤鎱ㄥ鍡楀⒒闁绘帞鏅幉绋款吋閸澀缃曢梻鍌欑閹碱偊宕锕€纾归柤鎭掑劜閸?6 缂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌ｉ幋锝呅撻柛濠傛健閺屻劑寮崹顔规寖闂佹椿鍘介悷鈺呭蓟閻斿皝鏋庨梽鍥敋瑜嶇叅妞ゆ挶鍨圭粻鏍归悩宸剰缂佺姴缍婇弻锝夊箛閵婏附鐝旀繛瀛樼矒缁犳牠寮诲☉妯兼殕闁逞屽墴瀹曟垵鈽夐姀鈥充簵濠电偞鍨崹娲偂閺囥垺鐓欓柡澶婄仢閹胶绱掗妸銈囩煓闁哄被鍔岄埥澶娾枎閹寸姷鏉芥繝娈垮枛閿曘儱顪冮挊澶屾殾妞ゆ劧绠戠粈瀣亜閹惧鈯曢柣搴ㄤ憾濮婄粯鎷呴崨濠傛殘闂佽鐡曞畷鐢靛垝濞嗘挸绠ｆ繝闈涙閵堫偄鈹戦敍鍕杭闁稿﹥鐗犻幃褍顭ㄩ崼婵堬紱濠电偞鍨跺銊︾▔瀹ュ洦鍠愰柣妤€鐗嗙粭姘舵煕婵犲倻浠涚紒缁樼洴楠炲鎮滈崱鏇犵闂備胶鎳撻顓熸叏閹惰姤鍊垮┑鐘插暔娴滄粓鏌熼幍铏珔闁逞屽墯椤ㄥ牊绔熼弴鐔洪檮闁告稑锕﹂崢鎼佹⒑閸涘﹤濮х紒鑼跺Г缁傚秹骞嗚閺€浠嬫煟閹邦厼绲婚柟顔藉灴閺岋綁鎮㈤崣澶嬬彋闂侀潧妫旂粈渚€锝炲┑鍫熷磯濞达絾娲忛崝搴ㄥ箟閸涘﹤绶為柟閭﹀墰閿涙盯姊洪悷鏉库挃缂侇噮鍨跺鏌ュ蓟閵夛妇鍘遍柣蹇曞仜婢т粙鍩婇弴銏＄厽婵犻潧娲ㄦ晶鍨叏婵犲啯銇濋柟绛圭節婵″爼宕ㄩ閿亾閸撗呯＝濞达絿鐡旈崵娆撴煟濡や焦灏い鏇稻缁绘繂顫濋鐐扮盎闂備胶绮幐鍛婎殽閹间焦鍊垫い鎾跺剱濞撳鏌曢崼婵囶棞濠殿啫鍛＜闁肩⒈鍓氬▍鏇烆熆鐟欏嫭绀嬮柛鈺嬬節瀹曟﹢顢旈崱顓犻棷婵犵數鍋犻幓顏嗙礊閳ь剚绻涢崪鍐偧闁轰緡鍠栭埥澶愬閿涘嫬寮抽梻浣告惈濞诧箓鏁嬮梺璇茬箚閺呯娀寮婚敐澶嬫櫜闁搞儜鍐ㄧ闂備浇顕栭崰鎺楀疾濠靛绠柣妯款嚙缁犵敻鏌熼悜妯烩拻鐎规洖鎼埞鎴︽晬閸曨偂鏉梺绋匡攻閻楃姴鐣峰┑鍠棃宕ㄩ鐘靛炊闂佺懓鍚嬮悾顏堝垂婵犳哎鈧?
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

    // 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柛鎰紦閻㈢粯淇婇悙顏勨偓鏍偋濡ゅ啰鐭欓柟鐐湽閳ь剙鎳樺畷锝嗗緞瀹€鈧鏇㈡⒑缁洖澧查柨姘舵煟韫囥儱顩柍褜鍓濋～澶娒洪弽褜鍤曢柛顐ｆ礀缁犵偤鏌曟繛鍨壔闁绘梻鍘ч崹鍌涖亜閺冣偓閸庡磭鏁崸妤佲拻?ARM 闂傚倸鍊搁崐鎼佸磹瀹勬噴褰掑炊瑜忛弳锕傛煕椤垵浜濋柛娆忕箻閺岀喖骞嗛弶鍟冩捇鏌涙繝鍌涘仴闁哄被鍔戝鎾倷濞村浜鹃柛婵勫劚椤ユ岸鏌涜椤ㄥ棝鎮″▎鎾村仯闁告繂瀚幆鍫熴亜閵壯冧槐闁哄本鐩俊鐑筋敊閻撳簶鎷婚梻浣芥〃缁€浣肝涘┑瀣摕婵炴垯鍨归崡鎶芥煏婵炲灝鍔氭い顐熸櫊濮婃椽宕ㄦ繝鍐ㄩ瀺婵犵數鍋涢敃銈夛綖韫囨稒鎯為柛锔诲幘閿涙粌鈹戦埥鍡楃仴鐎规洜鏁诲畷锝夊幢濡偐鐦堥梺闈涚箞閸ㄦ椽宕甸埀顒勬⒑閹稿孩澶勫ù婊勭矒閹儳鐣￠幍铏杸闁诲函缍嗘禍鐐哄礉閿曗偓椤啴濡堕崱妤冪懆闁诲孩鍑归崣鍐春濞戙垹绠ｉ柣鎰暩椤旀洟姊洪懖鈹炬嫛闁告挻绋撻弫顔尖槈濡攱鏂€闂佺粯鍔曞鍫曞窗濮椻偓閺岋紕浠﹂崜褎鍒涢悗娈垮櫘閸撴盯骞戦崟顖毼╃憸宥嗘叏闁秵鐓熼幖娣€ゅ鎰箾閸欏澧垫鐐寸墵椤㈡洟鏁傞挊澶夋偅婵犵數濞€濞佳囶敄閸涱垳鐭嗛悗锝庡亖娴滄粓鏌熼崫鍕棞濞存粓绠栧娲川婵犲啯鍎庨梺鍛婄懃閸燁偊锝炶箛鎾佹椽顢旈崟顐ょ崺濠电姷鏁告慨鎾疮椤愇诲洭鍩￠崒婊咁啎闁诲孩绋掗…鍥偝閼姐倗纾界€广儱瀚粣鏃傗偓瑙勬礀閹碱偊鍩ユ径鎰潊闁绘顕ч弫褰掓⒒娓氣偓濞佳呮崲閸儱鍨傚┑鐘崇閺?ABI闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸婂潡鏌ㄩ弮鍫熸殰闁稿鎸剧划顓炩槈濡娅ч梺娲诲幗閻熲晠寮婚悢鍏煎€绘俊顖滃劦閹峰綊姊虹粙璺ㄧ缂侇喖绉规俊鐢稿礋椤栨稒娅滈梺绯曞墲钃遍柡鍡欌偓顡玶sal 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柛鎰紦缂冩洟姊绘担鍛婃儓婵炲眰鍨藉畷褰掑捶椤撶姳绗夐梺缁橆焽缁垶鎮″▎鎾寸厽婵°倐鍋撻柣妤€锕﹀▎銏ゅ蓟閵夛妇鍘搁柣搴岛閺呮繄绮ｉ弮鍫熺厸閻忕偟鍋撶粈瀣偓瑙勬礈閸樠囧煘閹达箑鐐婇柤鍝ヮ暜妤犲繐鈹?GitHub 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙缁炬儳娼￠弻锝夊閵忊晝鍔搁梺姹囧€楅崑鎾舵崲濠靛洨绡€闁稿本纰嶉悘鎾绘⒑缂佹ê绗╁┑顔哄€楅幑銏犫槈閵忕姴鑰垮┑鈽嗗灥濞咃綁顢旂紒妯肩閻庢稒顭囬惌濠勭磽瀹ュ拑韬€殿喛顕ч埥澶娢熼崗鍏肩暦闂備線鈧偛鑻晶瀵糕偓瑙勬礃閸ㄥ潡骞冨▎鎾充紶闁告洦鍘介悡?
    // arm64 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愭い鏃傛嚀娴滈箖鏌涢幇闈涙灈妤犵偑鍨烘穱濠囨倷鐎涙顦ㄥ┑锛勫仩濡嫰顢氶敐鍡欘浄閻庯絽鐏氶弲婵嬫⒑缂佹ɑ顥嗘繛鎾诡潐缁旂喖寮撮姀鈾€鎷洪梺鍛婃崄鐏忔瑩宕㈠☉銏＄厱閻庯綆鍋呭畷宀勬煙閾忣偅宕岀€规洜顭堣灃濞达絽鎼獮宥嗕繆閻愵亜鈧牕螞娴ｅ摜鏆﹂柣銏犳啞閸庢绻涢崱妤冪畾闁衡偓娴犲鐓冮柍杞扮閺嗙偤鏌涙繝鍐ㄥ闁宠鍨块崹鎯х暦閸パ呭幗闁诲氦顫夊ú鏍偉婵傛悶鈧線寮崼婵堫槹闂佸壊鐓堥崑鎺懳涢妸褏纾介柛灞捐壘閳ь剚鎮傚畷鎰板箹娴ｅ摜锛欓梺褰掓？缁€浣哄瑜版帗鐓熼柟杈剧到琚氶梺绋匡工濞硷繝寮婚妸鈺佺睄闁稿本鐭竟鏇㈡倵鐟欏嫭绀冮柣鎿勭節瀵鈽夊Ο鍏兼畷闂侀€炲苯澧寸€规洘鍨甸埥澶娾枎濡粯鏆為梻鍌氬€搁崐鐑芥倿閿曞倸绠栭柛顐ｆ礀绾炬寧銇勯弽顐粶缂佲偓婢跺备鍋撻崗澶婁壕闂侀€炲苯澧柍璇查叄婵偓闁靛牆妫楅埀顒傚厴閹鈽夊▍顓т簻椤曪綁宕归瑙勬杸濡炪倖姊婚悺鏂库枔閺冣偓娣囧﹪顢曢敐搴㈢杹闂佺粯渚楅崰鏍亙婵炶揪缍€濞咃綁顢欓弴銏♀拺闁告挻褰冩禍婵堢磼鐠囨彃顏€规洘妞介弫鎰緞鐎ｎ剙骞愰柣搴″帨閸嬫捇鎮楅敐搴″鐞氾附淇婇悙顏勨偓鎴﹀垂閸濆嫀娑㈠礃閵娧勬闂佸憡顨堥崐锝夊籍閳ь剟骞忛崨顖涘仒闁炽儱鍘栨竟鏇㈡⒑鐠団€崇€婚柍褜鍓熷銊︾鐎ｎ偆鍘藉┑鈽嗗灥瀹曠敻骞夐崸妤佺厱閻庯綆鍋呭畷宀勬煛鐏炲墽銆掗柍褜鍓ㄧ紞鍡涘磻閸涱厾鏆﹂悘鐐靛亾閸欏繐鈹戦悩鎻掓殲闁靛洦绻冮〃銉╂倷閺夋垵顫嶉梺璇″灡濡啴寮幇鏉跨＜婵炴垼椴哥紞灞解攽閻樺灚鏆╁┑鐐╁亾濠电偘鍖犻崶褏锛欓梺鑽ゅ枛閸嬪﹤顭囬弽顓熺厾濠殿喗鍔曢埀顒佺墵閿濈偤寮撮姀锛勫幗闂佸綊鍋婇崳顕€寮抽鐐寸厱閻庯綆鍋呭畷宀勬煛娴ｇ懓濮堥柟顖涙閸ㄩ箖骞囨担褰掔崕闂傚倸鍊烽悞锕€顪冮崸妤€鍌ㄥù鐘差儏缁犳澘鈹戦悩瀹犲濡楀懘姊洪崨濠冨闁搞劋鍗冲畷姗€鍩€椤掑嫭鈷戦梻鍫熶腹濞戙垹绀嬫い鎰剁悼瑜邦垶姊?13 MB 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柟闂寸绾惧綊鏌熼梻瀵割槮缁炬儳缍婇弻娑樷攽閸曨偄濮夐梺绋款儐閹告悂锝炲┑瀣亗閹肩补妲呭鍓х磽閸屾瑧璐伴柛鐘愁殔铻為柛鎰靛枤瀹撲焦鎱ㄥ璇蹭壕濡炪們鍨洪悷鈺佺暦濡ゅ懎绀傚鎯邦潐瀹€鎼佸蓟閿濆棙鍎熼柍銉ュ暱鏉堝懘姊虹粙娆惧剱闁圭懓娲璇差吋閸偅顎囬梻浣告啞閹搁箖宕伴弽顓犲祦闁糕剝绋戦悞?5 MB闂?
    // AGP 婵犵數濮烽弫鍛婃叏閻戣棄鏋侀柟闂寸绾惧鏌ｉ幇顒佹儓闁搞劌鍊块弻娑㈩敃閿濆棛顦ョ紓浣哄С閸楁娊寮诲☉妯锋斀闁告洦鍋勬慨銏ゆ⒑濞茶骞楅柟鐟版喘瀵鎮㈤搹鍦紲闂侀潧绻掓慨鐢告倶閸儲鈷戠紒瀣劵椤箓鏌涢弮鈧崹鍨嚕婵犳碍鍋勯悶娑掆偓鍏呭闂佹眹鍨洪鏍倿妤ｅ啫鍐€闁斥晛鍟扮弧鈧?ABI 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙缂佺姷濞€閺岀喖骞戦幇闈涙闁瑰吋娼欓敃顏堝蓟閻旇櫣纾兼俊顖氭惈閻庮厼鈹戦悙鏉戠仸妞ゎ厼鍊垮顕€宕煎┑鍡欑崺婵＄偑鍊栭幐楣冨磻閻旂厧鐒垫い鎺戭槸瀵噣鏌″畝鈧崰鏍х暦濡ゅ懏鍤冮柍鍝勫€归鍐⒒娴ｈ鍋犻柛鏂跨焸閳ワ箓鎮滈挊澶嬫К?AAB 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋涢ˇ鐢稿极閹剧粯鍋愰柛鎰级閻ゅ嫬鈹戞幊閸娧呭緤娴犲鐤い鎰╁€愰崑鎾愁潩椤掑效闂侀潧娲ょ€氫即鐛幒妤€骞㈡俊鐐村劤椤ユ艾鈹戦敍鍕杭闁稿鍊栫粋宥咁煥閸繄鏌堥梺鍝勵槹閸ㄥ綊寮抽敃鍌涚厵闁绘垶锕╁▓鏇㈡煕婵犲嫬鍘撮柡宀€鍠撶槐鎺懳熼搹鍦噯婵犵數鍋涢悧鍕磹濠靛棭娼栫紓浣股戞刊鎾煕濞戞﹫宸ラ柡鍡楃墕閳规垿鎮欐０婵嗘疂缂備胶濮甸幐鎼佹偩閻戣棄绠ｉ柨鏇楀亾妤犵偑鍨虹换娑㈠幢濡搫袝濠电偠鍋愰崰鏍ь潖濞差亜绠伴幖娣灮椤︺儵姊虹粙鍖″伐缂傚秴锕獮鍐晸閻樺弬銊╂煥閺傚灝鈷旀い鏃€娲熷娲偡闁箑娈堕梺绋匡攻閻楃娀鐛€ｎ亖鏀介柟鎯у帨閸嬫捇宕稿Δ浣哄幐婵犮垼娉涢鎰板磿閵夆晜鐓熼柡鍌涘椤ョ娀鏌熼懠顒夌劸妞ゆ挸銈稿畷鍫曞煛閸愵亜甯梻鍌欑劍閻綊宕规繝姘瀬闁归棿鐒﹂崕妤€顭块懜闈涘闁绘挻鐟╅弻锝夊箣閻戝洣绶靛┑鐐茬墛濡啫鐣烽鍕垫晬闁绘劗琛ラ幏娲⒑閸︻収鐒鹃悗娑掓櫇婢规洟鎮介崨濠勫幈闁诲函缍嗛崑鍛暦瀹€鍕厸?AAB 闂傚倸鍊搁崐鎼佸磹閹间礁纾瑰瀣捣閻棗銆掑锝呬壕濡ょ姷鍋為悧鐘汇€侀弴銏℃櫇闁逞屽墴閹潡顢欐慨鎰盎闂佽婢樻晶搴ｇ矙缂佹绠鹃柛娑卞灠閳诲牊顨ラ悙瀵稿ⅹ閼挎劖銇勯幒鍡椾壕婵犵鈧偨鍋㈤柡?-PzippestBundle 闂傚倸鍊搁崐鎼佸磹閹间礁纾归柣鎴ｅГ閸ゅ嫰鏌涢锝嗙５闁逞屽墾缁犳挸鐣锋總绋款潊闁炽儱鍟跨花銉︾節瀵伴攱婢橀埀顒佸姍瀹曟垿骞樼紒妯煎帗闁荤姴娲╃亸娆愭櫏闁诲孩顔栭崰妤呭箰閹惰棄绠栭柍鍝勬噺閹偞銇勯幇鍓佺У闁哥偛鐖煎濠氬磼濞嗘垹鐛㈤梺閫炲苯澧伴柛瀣洴閹崇喖顢涘☉娆愮彿闂佸湱铏庨崰妤呮偂濞戞埃鍋撻崗澶婁壕闂侀€炲苯澧寸€规洜澧楅幆鏃堝Ω閿曗偓娴犲ジ姊虹捄銊ユ灁濠殿喚鏁诲畷鎴犫偓锝庡枟閻撴洘銇勯幇闈涗簻濞存粍绻冮妵鍕Ψ椤栨粎袦濠殿喖锕︾划顖炲箯閸涱垱鍠嗛柛鏇ㄥ亗閸濇姊绘担鍛婂暈闁哄被鍔戦幆宀勫磼閻愯尪鎽曟繝鐢靛Т濞诧箓寮查弻銉︾厱闁斥晛鍟粈鍫㈢磼?
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
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeUi")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeUi")
}
