# Zippest 视觉系统

克制的本地工具感：高留白、深青绿单点主行动、暖橙只承担加密与失败提示。

## Tokens

```css
:root {
  --bg: oklch(97.48% 0.0028 128.49);       /* #F6F7F5 */
  --surface: oklch(100% 0 0);              /* #FFFFFF */
  --fg: oklch(24.05% 0.0104 163.95);       /* #1B211E */
  --muted: oklch(48.67% 0.0137 161.64);    /* #59625D */
  --border: oklch(91.18% 0.0110 149.85);   /* #DDE4DE */
  --accent: oklch(47.03% 0.0926 166.26);   /* #0F6B4F */
  --warning: oklch(60.45% 0.1414 46.33);   /* #C4622D */
}
```

## Typography

- Display / body: `Noto Sans SC, Roboto, PingFang SC, Microsoft YaHei, sans-serif`
- Mono / technical values: `Roboto Mono, SFMono-Regular, Consolas, monospace`

## Observed rules

1. 页面背景使用 `--bg`，内容卡片与底部操作区使用 `--surface`，边界只靠低对比度 `--border` 与极轻阴影区分。
2. 圆角遵循 16 / 20 / 24 / 28dp 的层级；手机屏幕内圆角不超过 24dp，弹层可使用 28dp 顶角。
3. 深青绿只用于主按钮、进度、当前导航与可操作链接；暖橙只用于加密、失败与风险说明。
4. 版心左右留白 20dp，主要区块间距 16–24dp；列表行高稳定在 64–76dp。
5. 图标统一为 24dp 圆角线框，1.8px 描边；不使用表情符号或手绘插画。
