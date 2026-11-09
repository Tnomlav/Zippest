# design/current —— 当前态画板

`index.html` 是本项目的「现在长什么样」：左侧是代码里的 token（颜色 / 字阶），右侧是真机截图。
它不是设计稿，是**事实记录**——每次发版重拍一次即可。

## 怎么重拍

1. 手机连上（Windows 侧 `adb devices` 能看到设备，或在 WSL 里 `adb connect <手机IP>`）
2. 按下面的顺序截图，文件名保持现有约定，存到 `docs/screenshots/`：

   | 文件 | 画面 |
   |---|---|
   | `01-onboarding.png` | 首启权限页 |
   | `02-files.png` | 文件页（根目录，含最近解压） |
   | `03-tasks.png` | 任务中心 |
   | `04-trash.png` | 回收站 |
   | `05-compress.png` | 压缩设置面板 |
   | `06-progress.png` | 解压/压缩进行中（**当前缺失，待补**） |
   | `07-rename.png` | 快捷重命名 |
   | `08-password.png` | 批量密码 |

   ```bash
   adb exec-out screencap -p > docs/screenshots/02-files.png
   ```

3. 打开 `design/current/index.html` 确认图片正常，把页面顶部的日期改成采集日。

## 约定

- 只放真机/模拟器的原始截图，不放 HTML 原型截图；`design/*.html` 是 10 月冻结的原型，仅作历史参考。
- 截图里不要出现真实文件名、聊天记录、手机号等隐私内容。
- 深色模式与强调色各补一张更好；目前只拍了浅色 + 默认森林绿。
