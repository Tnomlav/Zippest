# Zippest 所有文件访问权限说明（应用商店用）

## 权限名称

`MANAGE_EXTERNAL_STORAGE`（Android 11+ 的「所有文件访问权限」）

## 为什么需要

Zippest 是压缩包工具，用户的核心操作是：打开手机上任意位置的压缩包（ZIP / 7Z / RAR / TAR / ISO / APK）、解压到目标目录、压缩任意文件夹、分卷合并、修复损坏的包。

这些操作要求应用能**读取用户选择的任意目录、并在用户选择的位置写入文件**。使用系统 SAF 目录选择器逐个授权时，每次换目录都要重新授权，无法完成「浏览整个存储 → 找到压缩包 → 解压到旁边」这一核心流程。

## 降级路径（权限被拒时）

应用提供「只选一个文件夹（更保守）」模式：用户通过系统目录选择器授权一个目录，Zippest 只在该目录内读写。功能受限但可用，且不申请「所有文件访问」。

## 数据与隐私

- 应用不申请 `INTERNET` 权限，所有文件处理在本机完成；
- 没有账号、没有广告、没有统计 SDK；
- 权限可随时在系统设置 → 应用 → Zippest → 权限 中撤销。

## 商店文案（可直接粘贴）

**中文（简短版）**

> Zippest 需要「所有文件访问权限」才能打开任意位置的压缩包并解压到您选择的位置。所有处理都在本机完成，应用不联网、不上传文件。您也可以只授权一个文件夹，功能会相应受限。

**English (short)**

> Zippest needs All files access to open archives from any folder and extract them where you choose. Everything is processed on-device; the app has no network permission and never uploads files. You can also grant a single folder instead, with reduced functionality.

## 提审截图建议

1. 首启权限说明页（说明用途 + 「只选一个文件夹」备选）；
2. 文件列表 → 打开一个压缩包；
3. 解压面板（选择目标目录）；
4. 关于页的「网络权限：无」与「开源许可：Apache-2.0」。

## 国内商店补充

华为/小米/OPPO/vivo 应用市场需在「必要权限说明」中填写同一段用途说明，并注明「拒绝后可用单目录模式」。
