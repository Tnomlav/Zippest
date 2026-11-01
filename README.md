# Zippest

Zippest 是一款面向 Android 的本地文件管理与压缩工具，交互方式参考
ZArchiver 和 RAR for Android。它既可以浏览、整理手机文件，也可以创建、
预览、分卷和后台解压常见的压缩格式。

![Zippest 文件浏览界面](design/screens/01-root.png)

## 项目结构

- `android/`：使用 Kotlin 和 Jetpack Compose 开发的 Android 应用
- `design/`：产品原型、图标和界面参考图
- `tools/`：本地测试素材与自动化脚本

## 主要功能

### 文件管理

- 浏览本地文件和文件夹，支持搜索、排序、新建目录、重命名和下拉刷新。
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
- 打开 ZIP、7Z 和 RAR 密码压缩包，并支持加密 7Z、RAR 头部解锁。
- 多压缩包任务可复用同一密码，也可逐个输入密码。
- 解压前可直接修改压缩包名称和后缀，支持 7Z、ZIP、RAR 与自定义后缀。
- 解压预览使用边框特效区分预览内容，点击预览文件可进入完整解压设置。
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
.\gradlew.bat assembleRelease
.\gradlew.bat bundleRelease
```

签名后的 Release APK 输出位置：

```text
android/app/build/outputs/apk/release/app-release.apk
```

可以直接使用 Android Studio 打开 `android/` 目录。发布签名配置位于本地、
不纳入 Git 的 `.signing/` 目录，发布前应同时备份密钥库及其配置。

## 当前版本

当前版本为 `1.5.10`，包名继续使用 `com.decompress.app`，以保持已有版本升级
兼容性。

此版本统一了删除确认弹窗的样式：显示删除数量，并提供标准的“取消”和警示色
“确认”操作。

完整版本历史与安装包请查看：

https://github.com/Tnomlav/Zippest/releases
