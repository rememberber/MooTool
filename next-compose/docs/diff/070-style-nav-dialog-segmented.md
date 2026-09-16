# DIFF-070：六套风格导航/弹层/分段选择器

- 编号：DIFF-070
- 影响：A01 外观（侧栏、设置、命令盘、弹层、分段）
- 日期：2026-09-15

## 原行为（Electron）

各 `data-interface-style` 块对 `.tool-button--active`、`.command-result--selected`、`.dialog`、`.command-palette`、`.settings-group__rows`、`.segmented` 有不同圆角、选中底与字色：

- hero：选中为强调色 14%/22% 叠底，弹层 16px，遮罩 0.46，分段大圆角
- claude：选中为 `--claude-accent-soft` / `--claude-accent-strong`
- miui-v5：选中白底 + 左侧橙色 inset
- modern：导航圆角 7px，弹层 13px，命令盘 8px，设置分组白底 8px，标题 18px

## 本产品行为

- `navSelectedFill` / `navSelectedContent` 接到侧栏、设置导航、命令盘结果、选中 Tab 按钮
- `dialogRadius` / `commandRadius` / `navRadius` / `settingsRowMin` 写入 `MooDimens`
- Git/历史/冲突/图片/HTTP 等剩余 overlay 改 `mooDialogSurface`
- hero 浅色遮罩与 Electron `dialog-backdrop` 同为 0.46
- 设置分类头 modern 18sp，其余 22sp

## 理由

此前选中导航一律 `selected` token + 3dp 条，弹层一律 12dp，hero 选中还走 raised 渐变，和风格块不一致。

## 证据

`ThemeContrastTest` 的 nav token / chrome radii。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、三平台安装仍未测。
