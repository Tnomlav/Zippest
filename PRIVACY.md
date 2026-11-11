# Zippest 隐私政策

生效日期：2026-11-09

Zippest 是一款在 Android 设备本地运行的文件管理与压缩工具。本政策说明
Zippest 如何处理设备权限、文件、剪贴板和后台任务信息。

## 数据收集与网络

Zippest 不要求注册账号，不包含广告、用户追踪或统计分析功能。

应用申请了 `INTERNET` 权限，但它只服务于一个用户主动触发的功能：在「关于」
页点按版本号时，向 GitHub Releases API 查询最新版本，并在用户确认后下载安装
包。除此之外 Zippest 不会主动联网，也不会上传文件、文件名、密码、剪贴板内容
或其他使用信息；用系统浏览器打开项目或许可证链接时，访问由浏览器完成。

该权限自 1.12.0 引入，更早的版本（≤1.11.x）未申请 `INTERNET`。

## 本地文件处理

文件浏览、压缩、解压、复制、移动、重命名、预览和完整性检查均在设备本地完成。
只有用户主动使用“打开方式”或“分享”时，所选文件或临时授权链接才会交给相应
系统应用处理。

## 权限用途

- 所有文件访问权限：直接浏览和修改内部存储及已挂载的外部存储。
- Storage Access Framework：访问用户明确选择的文件夹。
- 通知、前台服务和唤醒锁：在后台或熄屏时继续压缩、解压并显示任务进度。
- 网络：仅用于检查更新（详见上节「数据收集与网络」）。

Zippest 不会使用这些权限执行与用户所选操作无关的数据收集。

## 剪贴板与密码

应用仅在解压设置或用户主动点击剪贴板入口时读取当前剪贴板内容。密码可能暂存于
内存，或以 Android Keystore 管理的密钥加密后用于后台任务。应用不会在后台持续
监听或记录剪贴板变化，也不会上传剪贴板内容或密码。

## 回收站与本地记录

删除到回收站的文件、应用设置、任务记录和加密凭据均保存在本机应用数据区域。
用户可以通过回收站恢复或永久删除项目，也可以在 Android 系统设置中清除应用
数据。

## 第三方组件

压缩与解压功能使用本地开源组件。第三方组件仅在本机执行文件和格式处理，不提供
用户追踪或远程数据收集能力。详情见
[第三方开源许可](THIRD_PARTY_NOTICES.md)。

## 政策更新

如果后续功能引入联网、云服务或数据收集，本政策将同步更新。问题可通过
[GitHub Issues](https://github.com/Tnomlav/Zippest/issues) 联系项目维护者。

---

## English

Zippest is a local file and archive tool for Android. This policy explains how it
handles permissions, files, clipboard content and background tasks.

- **No accounts, ads, tracking or analytics.**
- **Network:** the app declares `INTERNET`, but only uses it when you tap the version
  number on the About page to query the GitHub Releases API. Files, file names,
  passwords and clipboard content are never uploaded.
- **Local processing:** files are read and written on the device through Android's
  storage APIs; nothing is sent to a server.
- **Clipboard:** read only when you open the password field, and only to offer the
  clipboard text as a candidate password; it is not stored.
- **Background tasks:** extraction and compression can continue in the background
  using WorkManager; task metadata (including an encrypted password when needed for
  retry) stays on the device.
- **Third-party components** are open-source libraries that run locally. See
  [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

If a future feature adds cloud services or data collection, this policy will be
updated. Questions: [GitHub Issues](https://github.com/Tnomlav/Zippest/issues).
