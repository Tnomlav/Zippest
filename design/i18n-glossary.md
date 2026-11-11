# Zippest i18n 术语表（英文附加）

> 本文件是 **第九章（§9 i18n）英文文案的唯一术语来源**。中文为主、英文附加；英文按中文原意重写，不做逐字直译。
>
> 英文界面统一 **Sentence case**：只有首词、专有名词和功能名首字母大写；按钮一律用动词。
>
> 最后更新：2026-11-12 · 对应 `docs/产品优化与调整指南-2026-11.md` §9 / I18N-12
> 相关文档：`docs/terminology.md`（中文用词）、`design/tokens.md`（视觉 token）

## 0. 使用约定

- 写新文案或改旧文案前先查本表；表里没有的概念，先在「核心术语」登记，再进代码。
- 中文用词以 `docs/terminology.md` 为准；本表只对英文负责。两处冲突时，中文听中文表，英文听本表。
- 中文列是现有界面用词，英文列是译名。**同一概念只用一个英文词，同一英文词只表达一个概念。**
- 英文资源放 `values/`（默认兜底），中文放 `values-zh/`；非中非英系统落到英文。
- 翻译不新增功能名：现有界面没有的「模式」「中心」「助手」等入口，不要为了英文造出来。
- `docs/terminology.md` 里「不要用 archive」是禁止中文界面夹英文；英文界面仍用 **Archive**，两处不冲突。
- README / PRIVACY 的双语分节见 I18N-20；LICENSE 全文不译。

## 1. 核心术语（18 条）

| 中文 | English | 说明 | 不要用 |
|---|---|---|---|
| 压缩包 | Archive | 泛指 ZIP、7Z、RAR、TAR 等；列表、包内浏览、任务标题都用它。格式名本身不译。 | zip file、package、compressed file、archive file |
| 解压 | Extract | 动作；按钮用 Extract、Extract selected。名词用 extraction。 | unzip、decompress、unpack、extract out |
| 压缩 | Compress | 动作；按钮用 Compress。名词用 compression。 | zip up、pack、archive（作动词） |
| 分卷 | Split volume | 多卷压缩包的一份；「分卷大小」= Split size，「不分卷」= No split。 | part、volume（单用）、split zip、multi-part |
| 密码 | Password | 输入框、加密选项、密码错误提示；不在通知或日志里回显。 | passcode、passphrase、pwd、code |
| 文件名编码 | Filename encoding | 设置项与乱码提示；选项值 Auto、GBK、Shift-JIS、UTF-8 不译。 | charset、character set、file name encoding |
| 条目 | Entry | 压缩包内的单个文件或目录，如 3 entries；文件列表的选中项用 item，不用 entry。 | item、element、object、file（目录也算条目时） |
| 目标位置 | Destination | 解压/压缩的去向；用于 Destination unavailable 等错误提示与设置项。中文面板里也叫「解压位置」，英文统一 Destination，不要另造 Extract location；具体路径才用 folder / path。 | target、output folder、save path、location |
| 同名文件 | Existing file | 冲突提示里的已有文件；完整句写 File already exists。 | duplicate、same-name file、conflict file |
| 覆盖 | Overwrite | 同名策略；按钮和选项都用 Overwrite。 | replace、cover、overwrite existing |
| 跳过 | Skip | 同名策略；按钮和选项都用 Skip。 | ignore、pass、bypass |
| 自动重命名 | Rename | 同名策略；普通重命名按钮也用 Rename，靠上下文区分。 | rename automatically、auto rename、keep both |
| 回收站 | Trash | 页面、菜单、设置项；Move to Trash、Trash is empty。功能名首字母大写。 | recycle bin、garbage、wastebasket、deleted items |
| 后台任务 | Background task | 单个任务；通知、任务行、状态用。任务中心是页面名，别混用。 | background job、worker、queue item |
| 任务中心 | Task center | 任务列表页面/入口；单个任务仍用 background task。 | task list、task queue、jobs、downloads |
| 快捷位置 | Quick location | 「快速访问」区块里的一个固定目录；区块名是 Quick access。 | shortcut、bookmark、favorite、pin |
| 权限 | Permission | 运行时权限与所有文件访问；All files access 是系统权限名，不拆译。 | authorization、access right、privilege |
| 仅预览 | Preview only | 状态说明：当前只能看，不能改或解压；不是功能入口名，不要造 Preview only mode。 | view only、read-only、just preview、preview mode |

表中是 Sentence case 的标签写法；放进句子后普通名词小写，例如 `Enter a password.`、`3 entries`、`File already exists.`。`Zippest` 与 `Trash` 保持原样。

易混边界：

- **Archive / Compress**：Archive 只作名词；动词一律 Compress。
- **Entry / item**：Entry 只在压缩包内；文件浏览的选中项用 item。
- **Destination / folder**：Destination 是「去哪」；具体目录才叫 folder，不要把路径写进按钮。
- **Trash / Delete**：可恢复用 Move to Trash；永久删除用 Delete forever，不要只写 Delete。
- **Quick access / Quick location**：Quick access 是区块，Quick location 是其中一个入口。
- **Task center / Background task**：Task center 是页面，Background task 是单个任务。

## 2. 文案规则

### 2.1 整句 + 占位符，禁止拼接

1. 一条资源 = 一个完整句子或一个完整语义单元。英文可以调语序，中文和英文不必逐词对应。
2. 变量一律带位置：`%1$s`、`%2$d`。只有一个变量也写 `%1$s`，不要裸 `%s`。
3. 类型固定：`%1$s` 只放字符串（文件名、文件夹名、压缩包名），`%1$d` 只放整数（数量、百分比）；不要用 `%1$s` 传数字。
4. **禁止拼接**：Kotlin 里不写 `"Extract " + name`、不写 `"$count" + " files"`，不要把「前缀 + 数量 + 后缀」拆成三条资源；英文里也不要用 `+` 组句。
5. 标点、空格、单位、引号随句子一起进资源；变量前后的空格写在资源里，调用处不再补。
6. 变量含义写进注释，多个变量按 `%1`、`%2` 顺序逐个说明。

```kotlin
// 不要：在调用处拼句子
Text("Extract " + archive.name)
Text("$count" + " files")
```

```xml
<!-- 位置：解压面板 → 目标位置；变量：%1$s = 目标文件夹名 -->
<string name="extract_destination_current">Extract to %1$s</string>

<!-- 位置：压缩完成提示；变量：%1$s = 压缩包名，%2$s = 保存位置 -->
<string name="compress_done">Created %1$s in %2$s.</string>
```

### 2.2 英文复数用 `<plurals>`

- 英文可数名词必须用 `<plurals>`；中文在同一 `<plurals>` 里只写一条 `other`（或 `one` / `other` 同文），不要为了英文给中文编两套说法。
- 英文只写 `one` / `other`：`1 file / 2 files`、`1 entry / 2 entries`、`1 task / 2 tasks`。不要写 `file(s)`、`1 files`、`1 file(s)`。
- 动词跟着变：`1 file was skipped` / `2 files were skipped`。
- 0 条用空态文案（`No files`、`Trash is empty`），不要用 `zero` 分支硬写 `0 files`。
- 选项列表用 `<string-array>`，顺序就是代码索引；改顺序必须同步代码。`<string-array>` 只放互斥选项，不放句子。

```xml
<!-- 位置：包内浏览 → 条目统计；变量：%1$d = 条目数 -->
<plurals name="archive_entry_count">
    <item quantity="one">%1$d entry</item>
    <item quantity="other">%1$d entries</item>
</plurals>

<!-- 位置：文件浏览 → 多选工具条；变量：%1$d = 已选条目数 -->
<plurals name="browser_selected_count">
    <item quantity="one">%1$d item selected</item>
    <item quantity="other">%1$d items selected</item>
</plurals>
```

### 2.3 按钮用动词，尽量 1–2 个词

- 动词优先：Extract、Compress、Rename、Delete、Overwrite、Skip、Retry、Cancel、Close、Unlock、Use、Apply。
- Sentence case：Extract all、Delete forever、Move to Trash；不要 Extract All、Delete Forever、Move To Trash。
- 不用 OK / Yes / No / Confirm / Submit 当主动作；按钮写具体动作。纯关闭用 Close，取消用 Cancel。
- 破坏性动作说清后果：可恢复写 Move to Trash，永久删除写 Delete forever；不要只写 Delete 让用户猜。
- 省略号只在还会弹窗或需要继续输入时用；不要给每个按钮都加 `…`。
- 英文比中文长约 30%，按钮优先 1 个词，2 个词是上限；3 个词只留给 Move to Trash 这类必须说清对象的场景，并用 `en-XA` 验证不截断。

### 2.4 错误句：what happened + what to do

1. 结构固定：第一句 what happened（具体、可核对），第二句 what to do（一个可执行动作，必要时给替代方案）。两句都用句号。
2. 不写 `Error:`、`Failed:`、`Oops`、`Sorry`、`Please`；不把异常原文、路径、堆栈、密码放进界面。
3. 不空泛：不要只写 `Operation failed.`。实在无法归类才写 `Something went wrong. Try again.`，并确保 Try again 真的可点。
4. 不承诺做不到的事：修复只对 ZIP 说；其他格式建议重新下载或换副本。
5. 不指责用户：写 `Wrong password`，不写 `You entered the wrong password`；写 `All files access is off`，不写 `You denied permission`。
6. 通知、Snackbar、对话框同一规则：标题可以短，正文必须给下一步。

| 现有中文 | 英文（推荐） | 要点 |
|---|---|---|
| 密码错误。加密 ZIP 注意大小写；7Z / RAR 头加密需要先解锁。 | Wrong password. Check the capitalization, or unlock header-encrypted 7Z/RAR archives first. | what + how |
| 目标位置不可用 | Destination unavailable. Choose another folder. | 不写 target |
| 存储权限被拒绝，可改为选择一个文件夹 | All files access is off. Turn it on in Settings, or choose a folder instead. | 系统权限名不译 |
| 缺少分卷。把同一目录下的其他分卷（.001 / .z01 / .part2.rar 等）补齐后再试。 | Missing split volume. Add the remaining split volumes (.001, .z01, .part2.rar, and so on) to the same folder, then try again. | 具体下一步 |
| 压缩包可能损坏或没下载完整。可以重新下载后再试，ZIP 还能试试修复。 | This archive may be damaged or incomplete. Download it again, or try Repair if it is a ZIP file. | 只对 ZIP 承诺修复 |
| 存储空间不足。清理一些文件，或换到空间更大的位置（比如 SD 卡）再解压。 | Not enough space. Free up storage, or extract to a larger location such as an SD card. | 给替代方案 |

### 2.5 不翻译

| 类别 | 保持原样 | 说明 |
|---|---|---|
| 许可证 | `LICENSE` | 文件名和全文都不译；「第三方开源许可」这个标题要译成 Third-party licenses。 |
| 格式名 | ZIP、7Z、RAR、TAR、TAR.GZ、GZ、BZ2、XZ、ZST 等 | 项目里 `CompressionFormat` 的写法原样保留；不要写 Zip、7z、Tar。 |
| 编码名 | GBK、Shift-JIS、UTF-8 | 选项值原样；「文件名编码」这个标签本身要译。 |
| 产品名 | Zippest | 不译、不加冠词，不写 the Zippest；Open with Zippest 译成「用 Zippest 打开」。 |

上述名称出现在句子里也不改写大小写；句首时保持原样，不为了 Sentence case 把 ZIP 改成 Zip。

## 3. 命名分组与注释

字符串 ID 统一为 `<group>_<object>_<role>`，全小写 snake_case。下表的示例 ID 是 P2 抽取时的建议，不是现有代码里已经存在的名字。

| 分组 | 覆盖范围 | 示例 ID | 说明 |
|---|---|---|---|
| `common_*` | 跨屏共用 | `common_cancel`、`common_close`、`common_retry` | 只有中文含义和英文完全一致才共用；不要为复用硬放。 |
| `browser_*` | 文件浏览：顶栏、排序、搜索、多选、空态、快速访问入口 | `browser_selected_count`、`browser_sort_name`、`browser_empty_folder` | 快速访问入口的展示归这里。 |
| `archive_*` | 打开压缩包后的包内浏览、密码、编码、预览、修复 | `archive_entry_count`、`archive_password_title`、`archive_repair_title` | 解压面板不在这里。 |
| `extract_*` | 解压面板、解压结果、解压通知 | `extract_destination_title`、`extract_conflict_title`、`extract_done` | 只覆盖解压链路。 |
| `compress_*` | 压缩面板、压缩结果、压缩通知 | `compress_format_title`、`compress_split_size`、`compress_start` | 只覆盖压缩链路。 |
| `task_*` | 任务中心、任务行、状态、通知 | `task_empty_title`、`task_pause`、`task_status_running` | 通知渠道名与正文也归这里。 |
| `trash_*` | 回收站页面与行内操作 | `trash_empty_title`、`trash_restore`、`trash_delete_forever` | 设置页里的回收站配置归 `settings_*`。 |
| `settings_*` | 设置页所有分区、选项、对话框 | `settings_language_title`、`settings_trash_retention`、`settings_permission_all_files` | 包括设置里的回收站、权限、快速访问编辑。 |
| `about_*` | 关于、隐私政策、第三方许可、版本 | `about_version`、`about_privacy_title`、`about_license_third_party` | 许可证正文不译，标题要译。 |
| `error_*` | 跨屏错误、通知错误、错误 Snackbar | `error_password_wrong`、`error_no_space`、`error_missing_volume` | 错误不按屏幕分组；同一错误只留一条。 |

`role` 词表：

- `_title`：页面、对话框、分区标题
- `_summary`：标题下的第二行说明
- `_label`：输入框、字段标签
- `_hint`：输入提示或 placeholder
- `_action`：按钮、菜单动作
- `_empty_title` / `_empty_body`：空态标题与正文
- `_count`：复数资源（配 `<plurals>`）
- `_options`：选项数组（配 `<string-array>`）

命名规则：

1. 全小写 ASCII snake_case；不用拼音、不用中文、不用 `btn` / `dlg` / `txt` / `msg` 这类缩写。
2. 前缀只能用上表 10 个；新组先评审，再进代码。
3. 一个 ID 只放一条完整文案；不要把标题和正文塞进同一条。
4. ID 稳定：只改英文措辞不改 ID；含义变了新建 ID，旧 ID 删除或标记废弃。
5. 复数用 `_count` 并配 `<plurals>`；选项用 `_options` 并配 `<string-array>`；不要用多条独立 string 在代码里拼数组。
6. 跨屏复用只进 `common_*`，且必须中英文含义都一致；只是「看起来差不多」就留在各自组。
7. 不在 ID 里写变量名、格式名或数字。
8. `translatable="false"` 只给确实不译的值（如格式名、编码名）；不要用它绕过 UI 文案翻译。

**硬性要求：每条 `<string>`、`<plurals>`、`<string-array>` 上方必须有一条注释，写清「出现位置」和「变量含义」；没有变量的写「变量：无」。** 注释跟着字符串走，抽取、移动、改文案时不能丢。

注释模板：

```xml
<!-- 位置：<屏幕/组件> → <区块>；变量：%1$s = <含义>，%2$d = <含义> -->
```

- 多个变量按 `%1`、`%2` 顺序写，和占位符一一对应。
- `<plurals>` 的注释写在整组上方，不写在每个 `<item>` 里；如果各 item 含义不同，说明拆错了。
- `<string-array>` 的注释要写顺序含义（索引对应什么），因为代码按下标取值。
- 注释用中文写即可，面向开发和翻译，不进界面；`values/` 与 `values-zh/` 保留同一条注释。
- 位置写到「屏幕 → 区块」级别，不要只写文件名；变量写「压缩包显示名」「目标文件夹名」「已选条目数」这种可核对的意思。

```xml
<!-- 位置：解压面板 → 目标位置；变量：%1$s = 目标文件夹名 -->
<string name="extract_destination_current">Extract to %1$s</string>

<!-- 位置：包内浏览 → 条目统计；变量：%1$d = 条目数 -->
<plurals name="archive_entry_count">
    <item quantity="one">%1$d entry</item>
    <item quantity="other">%1$d entries</item>
</plurals>

<!-- 位置：解压面板 → 遇到同名文件；变量：无；顺序：0 = 覆盖，1 = 跳过，2 = 自动重命名 -->
<string-array name="extract_conflict_options">
    <item>Overwrite</item>
    <item>Skip</item>
    <item>Rename</item>
</string-array>
```

## 4. 提交前自检

- [ ] 术语在本表里，英文与表一致
- [ ] 一条资源是整句，变量是 `%1$s` / `%2$d`，没有拼接
- [ ] 可数名词用了 `<plurals>`，没有 `(s)`
- [ ] 按钮是动词、Sentence case、1–2 个词
- [ ] 错误句有「发生了什么 + 怎么办」，没有异常原文
- [ ] 注释写了位置和变量，ID 属于 10 个前缀之一

## 5. 待确认

以下 5 条先按推荐值执行；I18N-19 英文审校时定稿，定稿后回写正文表格和所有资源。

| # | 中文 / 场景 | 候选 | 推荐 | 理由 / 待办 |
|---|---|---|---|---|
| 1 | 解压到（目标位置标签、按钮） | Extract to / Extract into / Extract in | **Extract to** | to 表示目的地，into 强调进入容器内部；目标是文件夹，不是压缩包内部。全项目统一 Extract to。 |
| 2 | 分卷（概念名、分卷大小、不分卷） | split volume / multi-volume archive / volume | **split volume**（Split size / No split） | volume 单用会和存储卷混淆；multi-volume archive 准确但长。概念名用 split volume，短标签用 Split size。 |
| 3 | 自动重命名（同名策略） | Rename / Auto-rename / Rename automatically | **Rename** | 一个动词，chip 里最短；上下文已说明是冲突策略。若评审坚持显式，用 Auto-rename 并全表替换，不要两种并存。 |
| 4 | 任务中心（页面 / 入口） | Task center / Tasks / Background tasks | **Task center** | 和单个 background task 区分。现有弹窗标题中文仍是「后台任务」，P2 抽取时按 `docs/terminology.md` 的「任务中心」对齐，英文不要跟着写 Background tasks。 |
| 5 | 仅预览（状态说明） | Preview only / View only / Read-only | **Preview only** | Preview 是现有动作；Read-only 会和文件权限混淆，View only 太泛。只作状态短语，不造 Preview only mode。现有界面没有这个入口，若后续新增入口需另立术语。 |
