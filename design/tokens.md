# Zippest 设计 token

> 这张表是设计系统的**唯一事实来源**，代码在
> `android/app/src/main/java/com/decompress/app/ui/theme/Tokens.kt` 与 `Theme.kt`。
> 改颜色 / 字阶 / 间距，先改这里，再改代码；不要在界面文件里写字面量。
>
> 最后更新：2026-11-12 · 对应 1.25.1

## 颜色

Material 角色（跟随深浅色与强调色）。`Light` / `Dark` 两套，强调色替换其中的 primary 三件套。

| 角色 | 用途 | Light | Dark |
|---|---|---|---|
| `background` | 页面底色 | `#F6F7F5` | `#121815` |
| `surface` | 卡片、底部操作区 | `#FFFFFF` | `#1B231F` |
| `onBackground` / `onSurface` | 主文字 | `#1B211E` | `#E8EEE9` |
| `surfaceVariant` | 次级块、输入框底 | `#E9EFEA` | `#2A342E` |
| `onSurfaceVariant` | 次要文字、说明 | `#59625D` | `#B8C4BB` |
| `outline` | 控件描边 | `#7D8C82` | `#66766B` |
| `outlineVariant` | 分隔线、极轻描边 | `#DDE4DE` | `#33403A` |
| `error` | 失败、加密、风险 | `#A24617` | `#F0A07A` |
| `errorContainer` | 错误提示底 | `#FBE7DC` | `#6B2A0F` |
| `primary`（默认森林绿） | 主按钮、进度、可操作 | `#0F6B4F` | `#7FD6B6` |
| `primaryContainer` | 选中态、轻量强调 | `#D9F2E7` | `#1D4B3A` |
| `secondary` | 次强调控件 | `#4F6B5E` | `#A9C7B8` |
| `secondaryContainer` | **FilterChip 选中态**、提示条底色 | `#DCE9E2` | `#2A3B34` |
| `tertiary` | 压缩包/格式图标的专属色 | `#41586A` | `#A8C0D4` |
| `surfaceDim` / `surfaceBright` | 深色层次两端 | `#DDE4DE` / `#FFFFFF` | `#121815` / `#2C3630` |
| `surfaceTint` | 高度染色 | 跟随主色 | 跟随主色 |
| `inverseSurface` / `inverseOnSurface` | Snackbar、反色提示 | `#2F3531` / `#F1F4F1` | `#E8EEE9` / `#1B231E` |
| `inversePrimary` | 反色主色 | 深色主色 | 浅色主色 |
| `scrim` | 弹层遮罩 | `#000000` | `#000000` |

> 2026-11-12 补：`error` 由 `#C4622D`（4.09:1）改为 `#A24617`（6.1:1），`outline` 由 `#B8C4BB`（1.80:1）改为 `#7D8C82`（3.5:1），并补齐 `inverse*` / `scrim` / `surfaceTint` / `errorContainer`；这几个角色之前走 Material 基线，Snackbar 的反色底色和弹层遮罩并不受控。
>
> 2026-11-09 补：`secondary` / `tertiary` 之前没定义，走了 Material 默认的紫色系，真机上表现为「选中的格式 chip 是淡紫、压缩包图标是暗紫」。现改为上面这组绿灰 / 蓝灰，与品牌色不打架。
>
> 同日补：`surfaceContainer*` 五个层级之前也没定义，`AlertDialog`、菜单、底部面板因此是淡紫底（`surfaceContainerHigh` 默认值）。现补齐：
> 浅色 `Lowest #FFFFFF / Low #F1F4F1 / Container #EBEFEB / High #E5EAE6 / Highest #DFE6E0`；
> 深色 `Lowest #0C110E / Low #171E1A / Container #1B231F / High #222B26 / Highest #2C3630`。

强调色（设置 → 外观）只替换 `primary / primaryContainer / onPrimaryContainer`，其余角色不变：

| 名称 | Light primary | Light container | Dark primary | Dark container |
|---|---|---|---|---|
| 森林绿（默认） | `#0F6B4F` | `#D9F2E7` | `#7FD6B6` | `#1D4B3A` |
| 海蓝 | `#1E5FA8` | `#DCE9F8` | `#8FC2FF` | `#1B3A5C` |
| 紫罗兰 | `#6A4C93` | `#EDE4F7` | `#C9AEF0` | `#3A2B52` |
| 琥珀 | `#A8561C` | `#F7E5D6` | `#F0B183` | `#4A2C16` |
| 玫瑰 | `#A8365A` | `#F8DFE6` | `#F2A6BC` | `#4C2030` |
| 石墨 | `#3F4A45` | `#E2E7E4` | `#C2CFC8` | `#2B342F` |

对比度（已测）：`#59625D` on `#FFFFFF` = 6.31:1，`#0F6B4F` on `#FFFFFF` = 6.49:1，均过 AA。

**用色规则**：青绿只用于主按钮、进度、当前导航与可操作链接；暖橙只用于加密、失败与风险说明。别用青绿做装饰。

## 字阶

13 档全部定义（此前只有 5 档，其余走 M3/Roboto 默认值，中英文混排行高不一致）。

| 样式 | 字号 / 行高 | 字重 | 典型用途 |
|---|---|---|---|
| `displaySmall` | 32 / 40 | SemiBold | 空态大标题（少用） |
| `headlineLarge` | 28 / 36 | SemiBold | 首启页主标题 |
| `headlineMedium` | 26 / 34 | SemiBold | 页面级大标题 |
| `headlineSmall` | 24 / 32 | SemiBold | 页面标题、对话框标题 |
| `titleLarge` | 22 / 28 | SemiBold | 顶栏目录名 |
| `titleMedium` | 16 / 24 | SemiBold | 分区标题、设置项标题 |
| `titleSmall` | 14 / 20 | Medium | 卡片内小标题 |
| `bodyLarge` | 16 / 24 | Regular | 正文强调 |
| `bodyMedium` | 14 / 20 | Regular | 默认正文 |
| `bodySmall` | 12 / 16 | Regular | 辅助说明 |
| `labelLarge` | 14 / 20 | Medium | 按钮文字 |
| `labelMedium` | 12 / 16 | Medium | 标签、角标 |
| `labelSmall` | 11 / 16 | Medium | 极小的脚注 |

字体：正文 `Noto Sans SC / Roboto`（系统字体，不自带）；技术数值用等宽 `Roboto Mono`。字号一律 `sp`，不要用 `dp` 写字号。

## 间距

4 dp 基线。代码里用 `ZippestSpacing`。

| Token | 值 | 用途 |
|---|---|---|
| `xs` | 4 | 行内元素间距 |
| `sm` | 8 | 行内元素、图标与文字 |
| `md` | 12 | 控件内边距、列表行内间距 |
| `lg` | 16 | 区块间距（默认） |
| `xl` | 20 | 页面版心左右留白 |
| `xxl` | 24 | 大区块间距 |
| `xxxl` | 32 | 分隔段、首启页 |
| `huge` | 40 | 大留白（空态） |

## 圆角

| Token | 值 | 用途 |
|---|---|---|
| `xs` | 8 | 小控件、输入框 |
| `sm` | 12 | 按钮、列表卡片 |
| `md` | 16 | 卡片、对话框 |
| `lg` | 20 | 面板 |
| `xl` | 24 | 大面板、底部操作区 |
| `sheet` | 28 | 底部弹层顶角 |
| `pill` | 999 | 胶囊标签 |

Material 组件默认形状（`ZippestShapes`）：extraSmall 8、small 12、medium 16、large 20、extraLarge 28。

## 尺寸与动效

| Token | 值 | 说明 |
|---|---|---|
| `iconSm` | 20 dp | 行内小图标 |
| `icon` | 24 dp | 标准图标（24 dp 线框，1.8 px 描边） |
| `touchTarget` | 48 dp | 可点区域下限 |
| `rowMin` | 64 dp | 列表行高下限（64–76） |
| `ZippestMotion.fast` | 120 ms | 点击反馈 |
| `ZippestMotion.normal` | 200 ms | 常规过渡、展开 |
| `ZippestMotion.slow` | 320 ms | 页面/面板入场 |

## 迁移状态

- 颜色：已全部走 `MaterialTheme.colorScheme`，界面文件里没有颜色字面量（`Theme.kt` 除外）。
- 字阶：已全部走 `MaterialTheme.typography`，界面文件里没有 `fontSize` 字面量。
- 间距与圆角：**尚未全面替换**。现状是 `8/12/16` 用得最多，`10/14/18` 与 `10/14/18/22 dp` 圆角属于历史值，后续改到哪个文件就顺手归到最近的 token（10→8/12、14→12/16、18→16/20），不要为新代码引入表外的值。
- `design/current/` 是真机截图的当前态画板，每次发版重拍一次；`design/*.html` 是 10 月冻结的原型，只作历史参考，**不再作为事实**。
