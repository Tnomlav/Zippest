---
name: zippest-design-language
description: Design or review any app UI so it reads as part of the Zippest family — restrained offline-tool aesthetic, warm paper background, one forest-green action colour, 64dp flat rows, tinted object-kind icons, 88% bottom sheets, one primary action per screen. Use for new screens, redesigns, design reviews, or HTML/Compose prototypes that must look and behave like Zippest. Style and interaction only; it contains no Zippest features.
en_name: Zippest Design Language
zh_name: Zippest 设计语言
en_description: Portable design language for building any app UI that reads as part of the Zippest family. Six layers — style, scale, composition, colour, usage logic, interaction logic — plus tokens and baseline boards. No product features.
zh_description: 让任意 App 与 Zippest 同系列的可移植设计语言。含风格、样式、构图、配色、使用逻辑、交互逻辑六层规范，附 token 与结构基准图；不含任何 Zippest 功能。
version: 1.0.0
tags: [design-system, design-language, mobile, material3, ui-kit]
od:
  mode: prototype
  platform: mobile
  category: design-system
  design_system:
    requires: false
  example_prompt: "用 Zippest 设计语言为相册 App 设计「时间线」首屏：先读 SKILL.md 与 references/04、05、07，产出 393×852 的可交互 HTML 原型，并用 references/09 自检。"
  example_prompt_i18n:
    zh-CN: "用 Zippest 设计语言为相册 App 设计「时间线」首屏：先读 SKILL.md 与 references/04、05、07，产出 393×852 的可交互 HTML 原型，并用 references/09 自检。"
    en: "Design an album app timeline screen in the Zippest design language. Read SKILL.md plus references/04, 05 and 07 first, produce an interactive 393×852 HTML prototype, then self-check with references/09."
---

# Zippest 设计语言（平台无关）

一句话：**像一件做得很顺手的本地工具，不像一个想让你多停留的产品。**

这套语言不描述任何具体功能。它只回答六件事：什么风格、什么样式、怎么构图、怎么配色、按什么逻辑使用、按什么逻辑交互。

## 0. 怎么用

1. 先读 §1 三条底线 + §2 十条铁律——它们决定"像不像"。
2. 按当前任务只读需要的那一份 references：

| 你在做什么 | 读 |
|---|---|
| 定颜色、选强调色、做深色 | `references/02-color.md` |
| 定字号、间距、圆角、动效 | `references/03-scale.md` |
| 排页面骨架、宽屏、弹层 | `references/04-composition.md` |
| 做行、按钮、栏、对话框 | `references/05-components.md` |
| 画图标、插画、加动效 | `references/06-icon-motion.md` |
| 定信息层级、入口、危险操作 | `references/07-usage.md` |
| 定选择、返回、反馈、长任务 | `references/08-interaction.md` |
| 收尾自检 | `references/09-checklist.md` |

3. 要落地时用 `assets/`：`tokens.css`（Web / HTML 原型）、`tokens.json`（任意技术栈）、`ZippestTheme.kt`（Jetpack Compose）。
4. 不确定时看 `examples/album-2.0.md`：同一套语言套到一个相册 App 上的完整推演。
5. 想知道每条规则从哪来，看 `SOURCES.md`。

## 1. 三条底线

违反任何一条，就不再是同系列，改别的都没用。

1. **一个强调色。** 屏幕上所有彩色只允许来自主色与语义色；主色是唯一的"行动色"，其余位置一律中性。
2. **扁平优先。** 并列内容用扁平行 + 1px 分隔线，不用卡片堆叠；层级靠留白、色阶和描边，不靠阴影。
3. **一屏一个主动作。** 每屏只有一个视觉上最重的按钮；进入选择 / 编辑态后，原来的主入口必须让位。

## 2. 十条铁律

| # | 铁律 | 参数 |
|---|---|---|
| 1 | 底色是暖白纸，不是纯白 | `#F6F7F5` / 深色 `#121815` |
| 2 | 表面比底色更亮一档 | `#FFFFFF` / 深色 `#1B231F` |
| 3 | 主色单点使用 | `#0F6B4F`，只给主按钮、进度、当前项、可点链接 |
| 4 | 警示色只给风险 | `#A24617`，只给失败、破坏性、风险提示 |
| 5 | 行高 64，分隔线 1px | 列表行 ≥64dp；分隔线 = `outline` 32% |
| 6 | 版心 20 | 面板与设置左右 20dp；列表左右 12dp |
| 7 | 圆角五级 | 8 / 12 / 16 / 20 / 24；弹层 28；胶囊 999 |
| 8 | 图标 24 网格 | 24dp 线框图标；容器 40dp、圆角 12 |
| 9 | 触达 48 | 任何可点区域 ≥48dp |
| 10 | 动效只有三档 | 120 / 200 / 320ms，没有无限装饰动画 |

## 3. 十二个家族指纹

别人判断"这是不是同一系列"，靠的是这些可指认的特征，而不是形容词。

| # | 指纹 |
|---|---|
| 1 | 暖白底 + 纯白表面的两级色阶，几乎没有可见阴影 |
| 2 | 深森林绿只出现在一个地方：当前可做的那个动作 |
| 3 | 类型图标是"语义色方底 + 24dp 线框图标"，40dp / 圆角 12 |
| 4 | 并列内容是一行行，不是一张张卡；行与行之间只有一条 1px 线 |
| 5 | 顶栏与页面同底色，没有 elevation，没有渐变 |
| 6 | 底部只有一条常驻带：状态条、操作条、进度条三者取其一 |
| 7 | 配置类操作从底部升起，最高占屏 88%，顶角 28 |
| 8 | 空态是一张单色线性小画 + 主色一点点，不用人物 / 3D / 渐变 |
| 9 | 所有数字等宽，进度、大小、时间刷新时不跳 |
| 10 | 标题左对齐、字重 SemiBold，没有居中大标语 |
| 11 | 动效短、直、不弹跳；没有呼吸光、没有循环 shimmer |
| 12 | 一屏看不到第二种强调色，也看不到第二个主按钮 |

## 4. 30 秒决策树

- **这屏的主动作是什么？** 只留一个；其余降级为图标、文字按钮或"更多"。
- **内容是一条条并列的？** 扁平行 + 分隔线。不要给列表套卡片。
- **要配置参数？** 88% 底部面板。**要确认后果？** 对话框。**要换个地方？** 整页。
- **有对象类型？** 用语义色方底图标，不要随手给每类一个彩色。
- **有长任务？** 后台队列 + 浮动进度条 + 任务页，三件套。
- **空 / 载 / 错？** 用三套模板，不要自创第四种。
- **屏幕 ≥600dp？** 加左导航栏；≥840dp 再加第三栏。不要把单栏内容拉宽。

## 5. 落地顺序

1. 铺 token（颜色 → 字阶 → 间距 / 圆角）。
2. 定骨架（顶栏 64 / 内容 / 底部一条带）。
3. 做行与语义图标。
4. 做面板、对话框、菜单。
5. 补空 / 载 / 错三态与动效。
6. 跑 `references/09-checklist.md`，不过就改。

## 6. 去功能化约定

本 skill 不写功能。写规则时用下面左列的名词；一旦出现具体功能名，说明写歪了。

| 通用词 | 含义 | 在你的 App 里可能是 |
|---|---|---|
| 条目 item | 列表里的一行 | 照片、文档、歌 |
| 对象 object | 被整体处理的东西 | 项目、相册、歌单 |
| 批次 batch | 多选后的一批条目 | 一批照片、一次导入 |
| 长任务 long task | 后台持续运行的操作 | 导入、导出、转码、上传 |
| 面板 sheet | 收集参数的底部层 | 导出设置、滤镜参数 |
| 主操作 primary action | 这屏唯一的实心按钮 | 开始导入、应用、保存 |

## 7. 文件地图

```
SKILL.md                      入口与铁律（你正在读）
AGENT.md                      接入提示词（贴给其他 Agent 用）
README.md                     安装、版本、维护方式
SOURCES.md                    每条规则的证据出处
references/00-identity.md     家族指纹的展开与反例
references/01-style.md        风格、材质、密度、语气
references/02-color.md        语义配色、深色、强调色
references/03-scale.md        字阶、间距、圆角、尺寸、动效
references/04-composition.md  页面骨架、宽屏、弹层
references/05-components.md   行、按钮、栏、对话框、菜单
references/06-icon-motion.md  图标、插画、动效
references/07-usage.md        信息层级、入口、危险操作、文案
references/08-interaction.md  选择、返回、手势、反馈、长任务
references/09-checklist.md    20 项自检 + 12 种走样
assets/tokens.{json,css}      机器可读 token
assets/ZippestTheme.kt        Compose 落地
assets/baseline/              4 张结构基准图（SVG）
examples/album-2.0.md         换皮推演：相册 App
```

## 8. 交付前

至少通过：三条底线 + 十条铁律 + `09-checklist.md` 的 20 项。三者任一不过，不要交付。
