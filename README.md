# Zippest

Zippest 最独特的两项能力是便捷的后缀重命名与批量密码填充：重命名时会自动
选中最后一个点及其后的后缀，并提供 7Z、ZIP、RAR 和自定义后缀快捷替换；
批量解压多个压缩包时可启用“使用相同密码”，一次填写后自动应用到整个任务，
无需逐个输入。在此基础上，Zippest 还把文件管理、压缩、解压和删除整合成
不跳转系统文件管理器的连续工作流，支持复制或移动后停留在目标文件夹、熄屏
和后台继续处理、实时进度浮窗、任务暂停与恢复、持久回收站及分卷操作。

![Zippest 文件浏览界面](design/screens/01-root.png)

## 项目结构

- `android/`：使用 Kotlin 和 Jetpack Compose 开发的 Android 应用
- `design/`：产品原型、图标和界面参考图
- `tools/`：本地测试素材与自动化脚本

## 主要功能

### 文件管理

- 浏览本地文件和文件夹，支持搜索、排序、新建目录、重命名和下拉刷新。
- 搜索可切换「当前文件夹 / 全部存储」：全部模式会扫描整个内部存储并显示文件所在路径。
- 长按进入多选，可复制、移动、分享、压缩、解压、删除及查看打开方式。
- 复制和移动直接在当前文件页选择目标目录，完成后停留在目标文件夹。
- 删除前显示一次确认，默认移入回收站，可从文件页菜单恢复或永久删除。
- 支持直接内部存储访问，也保留 Android Storage Access Framework
  文件夹授权作为兼容方案。
- 内部存储快捷位置支持通过系统文件管理器新建、改址和删除。

### 压缩与解压

- 浏览并解压 ZIP、7Z、RAR、TAR、TAR.GZ、TAR.BZ2、TAR.XZ、
  TAR.ZST、TAR.BR、CPIO、GZ、BZ2、XZ、LZMA、LZ4、ZST 和 BR。
- 创建 ZIP、7Z、TAR 系列、CPIO、GZ、BZ2、XZ、LZMA、LZ4 和 ZST
  压缩包，并可选择压缩级别。
- 创建带密码的 ZIP 和 7Z 压缩包。
- 将支持的压缩包分卷为 `.001`、`.002` 等格式，或创建
  `.z01`、`.z02`、`.zip` 分卷集合。
- 打开分卷压缩包的第一个文件时，会自动读取同目录中的全部后续分卷；
  支持数字分卷、`.partN.rar`、`.000.rar`、RAR5 以及旧式 `.rar/.r00`。
- 未加密的 7Z、RAR4 和 RAR5 使用 NDK 原生 libarchive 引擎解压，
  加密数据或加密文件头会自动回退到兼容性引擎。
- 打开 ZIP、7Z 和 RAR 密码压缩包，并支持加密 7Z、RAR 头部解锁。
- 多压缩包任务可复用同一密码，也可逐个输入密码。
- 解压前可直接修改压缩包名称和后缀，支持 7Z、ZIP、RAR 与自定义后缀。
- 解压预览使用边框特效区分预览内容，点击预览文件可进入完整解压设置。
- 包内点按文件会只解出该条目并用系统应用打开；长按可多选，底部「解压所选」只解选中的文件。
- 解压或压缩完成后，通知栏与页面内 Snackbar 都能直接打开结果文件夹。
- TAR / TAR.GZ / TAR.BZ2 / TAR.XZ 支持断点续传：进程被系统杀掉后，重新打开应用会从上次写到的条目继续，而不是从头再压一遍。
- 支持完整性检查，并防止路径穿越、异常条目数量和磁盘空间不足等问题。

### 后台任务

- 使用 WorkManager 排队执行压缩和解压任务。
- 应用进入后台或屏幕熄灭后，任务可继续运行。
- 提供常驻通知、应用内任务中心和浮动进度条。
- 支持暂停、继续、取消与恢复检查点。
- 队列密码使用 Android Keystore 管理的密钥加密。

### 界面与系统集成

- 支持系统主题、手动浅色和深色主题。
- 图片和视频显示缩略图，普通文件支持调用系统应用打开。
- 打开方式优先展示查看器和编辑器，其次才是分享或发送类应用。
- 相册和播放器应用打开媒体时，可访问同文件夹内的其他媒体。
- 文件菜单、排序菜单、内部存储面板和选择菜单采用互斥显示。
- 支持 Android 分享、文件关联和“用 Zippest 打开”流程。

## 构建

```powershell
cd android
$env:JAVA_HOME='C:\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat lintDebug
.\gradlew.bat assembleRelease          # 产出 arm64 / v7a / universal 三个 APK
.\gradlew.bat -PzippestBundle bundleRelease   # 打 AAB（AGP 不允许与 ABI 拆分同时构建）
.\gradlew.bat -PzippestEmulator assembleDebug # 模拟器联调包（x86_64）
```

签名后的 Release APK 输出位置：

```text
android/app/build/outputs/apk/release/app-arm64-v8a-release.apk
android/app/build/outputs/apk/release/app-armeabi-v7a-release.apk
android/app/build/outputs/apk/release/app-universal-release.apk
android/app/build/outputs/bundle/release/app-release.aab
```

可以直接使用 Android Studio 打开 `android/` 目录。发布签名配置位于本地、
不纳入 Git 的 `.signing/` 目录，发布前应同时备份密钥库及其配置。

## 当前版本

当前版本为 `1.7.1`（versionCode 74），包名继续使用 `com.decompress.app`，以保持已有版本升级
兼容性。

此版本给 TAR / TAR.GZ / TAR.BZ2 / TAR.XZ 加上压缩断点续传：压缩时先在目标目录写一个
隐藏临时文件，按 64 MB 分段记录进度；进程被系统杀掉后，下次启动会从断点截断续写，
完成后再改名为正式文件。ZIP 与 7Z 的目录结构在文件末尾，暂不支持续传，界面会明确提示。

此版本补齐了压缩包内的操作：点按包内文件可直接用系统应用打开（只解出这一个条目），
长按可多选并「解压所选」；解压与压缩完成后，通知栏与页面内的 Snackbar 都能直接
「打开所在文件夹」。

此版本给搜索加上「当前 / 全部」范围切换：切换到「全部」会扫描整个内部存储，
结果显示所在路径，文件夹与压缩包都可直接打开。全盘搜索跳过 `Android/` 与隐藏
目录，最多扫描 25k 个条目，输入有 250 ms 防抖。

此版本把安装包从 7 个 ABI 收敛到 `arm64-v8a` 与 `armeabi-v7a`：arm64 单包
4.87 MB、v7a 4.47 MB、通用包 7.01 MB，AAB 7.71 MB，并已通过 16 KB 页面
对齐校验。

此版本新增 tag 触发的发布流水线（`.github/workflows/android-release.yml`）：
校验 tag 与 versionName、构建分 ABI 的 APK 与 AAB、检查载荷与 16 KB 对齐、
校验签名，并把产物与 SHA256SUMS 一起发布。

完整版本历史与安装包请查看：

https://github.com/Tnomlav/Zippest/releases

## 隐私与许可

- [隐私政策](PRIVACY.md)
- [第三方开源许可](THIRD_PARTY_NOTICES.md)
- [项目许可证](LICENSE)（Apache-2.0）
- Zippest 不申请 `INTERNET` 权限，文件、密码与文件名都不会离开本机；可在系统设置的应用权限页核实。
