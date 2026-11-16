# Zippest 设计语言 Skill

把一个 App 的界面做成"Zippest 同系列"的可复用规范：风格、样式、构图、配色、使用逻辑、交互逻辑。
**不含任何 Zippest 功能**——没有具体的文件操作流程，也没有业务规则。

- 版本：1.0.0
- 基线：Zippest Android 1.54.0（2026-11-14）
- 平台：平台无关；附 Web 与 Jetpack Compose 两种落地 token

## 这是给谁用的

| 角色 | 怎么用 |
|---|---|
| 设计 / 前端 | 读 `SKILL.md` + 按需读 `references/`，用 `assets/tokens.css` 起页面 |
| Android | 直接拷 `assets/ZippestTheme.kt`（改包名），按 `references/05` 搭组件 |
| AI Agent | 把本目录作为 skill 加载；`SKILL.md` 是入口，按其中的任务表跳转 |
| 评审 | 只用 `references/09-checklist.md` 的 20 项打分 |

## 目录

```
SKILL.md            入口：三条底线、十条铁律、十二个指纹、决策树
AGENT.md            接入提示词：贴给 OpenDesign 等 Agent 的完整版 / 精简版
references/         六层规范（风格 / 样式 / 构图 / 配色 / 使用 / 交互）
assets/             token（json / css / Compose）+ 4 张结构基准图 + 预览页
examples/           换皮推演：相册 App（Album 2.0）
SOURCES.md          每条规则的证据出处
```

## 四种用法

**1. 作为规范读**
先读 `SKILL.md` 的 §1–§3；然后只读你当前在做的那一层（见 §0 的任务表）。不要一次读完 10 份 references。

**2. 作为代码资产用**
- Web / HTML 原型：`<link rel="stylesheet" href="assets/tokens.css">`，用 `.z-*` 类起步。
- Compose：复制 `assets/ZippestTheme.kt`，改 `package`，在 `setContent { ZippestTheme { ... } }` 外层包裹。
- 其他栈：`assets/tokens.json` 是唯一数值来源，自行映射。

**3. 作为 Agent skill 安装**
`SKILL.md` 带 YAML frontmatter（`name: zippest-design-language`）。
如果目标工具的 skill 目录要求"文件夹名 = skill 名"，把本目录重命名或软链为 `zippest-design-language` 再放进 skills 根目录即可，内容不用改。

**4. 作为评审标准用**
只跑 `references/09-checklist.md`：20 项 + 5 问家族辨识测试。两项以上不过，回到 `01-style.md` 查方向。

## 接到别的 Agent（OpenDesign 等）

只对它说"参考 Zippest 风格"不会生效，它会自由发挥。要让它**按这套语言做**，必须给它三样东西：入口、路径、硬约束。
`AGENT.md` 已经写好两种可直接粘贴的提示词：

| 对方能力 | 用哪一版 | 还要做什么 |
|---|---|---|
| 能读本地文件 | 完整版 | 确认路径 `C:\Album2.0\skill` 正确；把提示词放进项目规则/系统提示 |
| 有知识库或资料上传 | 完整版 | 上传整个 `skill/` 目录，至少 `SKILL.md` + `02` + `03` + `05` + `09` |
| 只有对话框 | 精简版 | 直接粘贴；复杂页面把 `04`、`05` 的关键段也贴进去 |

验收时让它按 `09-checklist.md` 输出一张"检查项 / 结果 / 证据"的表，否则它会在"看起来差不多"时收工。

### 在 Open Design 客户端里装（0.27.x）

它原生认 `SKILL.md`：安装时在 `%APPDATA%\Open Design\namespaces\release-stable-win\data\skills\` 下建一个指向源目录的 junction（目录联接），技能 id 取 frontmatter 的 `name`。所以本目录叫 `skill` 不影响显示名，也不会复制出一份副本。

1. 打开 Open Design → **技能 / Skills** → **安装 / Install**
2. 来源选 **本地文件夹 / Local folder**
3. 选 `C:\Album2.0\skill` → 安装

装完检查三件事：

- 列表里出现 **Zippest 设计语言**（来自 `zh_name`），描述是中文那句（来自 `zh_description`）。
- 卡片上的「使用此提示」用的是 frontmatter 里的 `od.example_prompt`。
- 在任意项目的这一轮里**附加该技能**，否则它不会进上下文——Open Design 是按轮次挂技能的。

因为是 junction，之后我改 `C:\Album2.0\skill` 里的任何文件，客户端读到的就是新内容，不用重装。
frontmatter 已声明 `od.design_system.requires: false`，所以它不会被要求再挂一个设计系统。

## 维护（重要）

这套 skill 是 **Zippest 设计系统在 1.54.0 的快照**，不是活文档。上游改动后按下面顺序同步：

1. 对比 Zippest 的 `ui/theme/Theme.kt`、`Tokens.kt`、`design/tokens.md` 与本目录 `assets/tokens.json`。
2. 只同步"角色值"，不同步功能：新增的颜色 / 字号 / 间距进 json，再改 `02`、`03` 的表格。
3. 交互规则变了，先改 `07` / `08`，再回头检查 `09` 的检查项是否仍然成立。
4. 改了基准图就更新 `assets/baseline/` 与 `examples/`。
5. 升版本号，并在本文件顶部更新"基线"。

**不要**把 Zippest 的页面截图直接塞进来当规范：截图会过期，规则不会。

## 边界

- 不管品牌 logo、商店素材、营销文案、插画外包。
- 不管功能取舍：只规定"长什么样、怎么用"，不规定"做什么"。
- 不承诺像素级还原某个具体版本；它保证的是"同系列"，不是"同一张图"。

## 出处

规则来自 Zippest 的代码、设计 token、真机复盘与版本记录，逐条索引见 `SOURCES.md`。
代码与文档以 Apache-2.0 发布；本 skill 为其设计语言的可移植提炼。
