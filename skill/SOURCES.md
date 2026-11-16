# 证据索引

本 skill 的每条规则都能回到 Zippest 仓库（基线 1.54.0）的一个具体位置。
路径相对于 Zippest 仓库根目录；`ui/` = `android/app/src/main/java/com/decompress/app/ui/`。

## 颜色

| 规则 | 出处 |
|---|---|
| 浅色 / 深色全部 M3 角色 | `ui/theme/Theme.kt`（LightColors / DarkColors） |
| 角色用途与对比度 | `design/tokens.md` §颜色（含 2026-11-12 的对比度修正记录） |
| 六套强调色 | `design/tokens.md` §强调色；`data/AppSettings.kt` 的 `AppAccent` |
| 错误色 #A24617、outline #7D8C82 | `design/tokens.md` 注释（4.09:1 → 6.1:1；1.80:1 → 3.5:1） |
| 选中行 primaryContainer 45% | `ui/FileListViews.kt` EntryRow |
| 分隔线 outline 32% | `ui/FileBrowserViews.kt`、`ui/FileListViews.kt` |
| 类型色映射 | `ui/theme/FileKindIcons.kt` |

> 已知漂移：`surfaceContainer*` 五级在 `design/tokens.md`（High `#E5EAE6`）与 `Theme.kt`（High `#FFFFFF`）不一致。本 skill 以 **1.54.0 的代码**为准；差异不影响任何界面规则，只影响深色/骨架的色阶精度。读到这段时，如果上游已统一，按新值更新 `tokens.json` 即可。

## 字阶 / 间距 / 圆角 / 动效

| 规则 | 出处 |
|---|---|
| 13 档字阶、全 `tnum` | `ui/theme/Theme.kt` AppTypography |
| 间距 4–40、圆角 8–28 | `ui/theme/Tokens.kt` ZippestSpacing / ZippestRadius / ZippestSize |
| 动效 120 / 200 / 320 | `ui/theme/Tokens.kt` ZippestMotion |
| 行高 64、图标 24、触达 48 | `ui/theme/Tokens.kt` ZippestSize；`ui/FileListViews.kt` |
| 数字等宽与 4dp 门禁 | `docs/产品优化与调整指南-2026-11.md` §8.2、§8.3 |

## 构图与组件

| 规则 | 出处 |
|---|---|
| 顶栏同底色、无阴影 | `ui/FileBrowserTopBar.kt`（containerColor = background） |
| 列表左右 12、行内 12/8 | `ui/FileBrowserViews.kt`、`ui/FileListViews.kt` |
| 类型图标 40 / 圆角 12 | `ui/ThumbnailViews.kt` FileKindIcon |
| 缩略图 44 / 圆角 12 | `ui/ThumbnailViews.kt` MediaThumbnail |
| 底部三选一（状态 / 操作 / 传输） | `ui/FileBrowserBottomBar.kt` |
| 选择工具条 ≥64 + 阴影 8 | `ui/SelectionToolbar.kt` |
| 浮动进度条（圆角 16 / 阴影 12 / 进度 5） | `ui/TaskCenterViews.kt` ActiveArchiveTaskBar |
| 面板 88%（表单 90%）、skipPartiallyExpanded | `ui/ArchiveScreen.kt:1267`、`ui/TaskCenterViews.kt:120`、`ui/TrashSheet.kt:64`、`ui/CompressionSheet.kt:258` |
| 对话框圆角 28 / 图标 58 / 按钮 48 | `ui/FileActionViews.kt` DeleteConfirmationDialog |
| 设置分组（label + 圆角 16 白卡 + 行 16） | `ui/SettingsScreen.kt` SettingsSection / SettingsChoice |
| 空态锚在 30%、骨架 300ms、错误模板 | `ui/StateViews.kt` StatePanel / LoadingSkeleton / ErrorTemplate |
| 插画 72 / 线宽 1.5 / 主色点缀 | `ui/EmptyIllustrations.kt` |
| 宽屏 240 + 320 三栏 | `ui/BrowserNavPane.kt`、`ui/FileBrowserScreen.kt`（width 240） |

## 使用与交互

| 规则 | 出处 |
|---|---|
| 打开即内容、层级 ≤3 | `design/README.md` §信息架构；`docs/产品优化与调整指南-2026-11.md` §2.1 |
| 一屏一主动作、去掉 FAB | 指南 §8.7（1.25.0 落地） |
| 选择模型（长按 / 全选再点清空 / × 退出） | `ui/FileBrowserState.kt`、`ui/FileBrowserTopBar.kt`、`ui/SelectionToolbar.kt` |
| 返回逐层退、只保留系统手势 | 指南 §8.7（删除自绘边缘返回区） |
| Snackbar 底部 8 / 左对齐 / ≤2 行 | 指南 §8.7；`ui/CenteredSnackbarHost.kt` |
| 批默认复用一次输入 | `docs/release-notes/1.24.0.md`（多包密码复用） |
| 破坏性选项靠后 | `docs/release-notes/1.47.0.md`（重命名 → 跳过 → 覆盖） |
| 长任务三件套（队列 / 浮动条 / 任务页） | `ui/TaskCenterViews.kt`、`docs/release-notes/1.23.0.md` |
| 文案语气与术语 | `design/i18n-glossary.md`、`docs/terminology.md` |
| 无障碍（48dp / 合并语义 / 200%） | 指南 §8.8 |

## 家族指纹的形成

| 指纹 | 依据 |
|---|---|
| "纸感工具"的定位 | 指南 §8.0 总判断 |
| 先统一再谈好看 | 指南 §8.11 末段 |
| "看起来像三个人做的"问题 | 指南 §8.0 原始判断 |
| 禁止硬编码色 / 表外间距 | 指南 §8.1、§8.3 的 preflight 门禁 |

## 明确不作为依据

- `design/*.html`：2026-10 冻结的交互原型，只作历史参考（`design/tokens.md` 明确声明）。
- `docs/screenshots/`：截图停在 1.2x，底部 Tab 等结构已被移除，只用于理解气质，不用于取值。
- `tools/prompts/*.md`：早期生成提示词，其中的琥珀色文件夹等要求已被现版语义色取代。
