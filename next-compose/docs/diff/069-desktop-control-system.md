# DIFF-069：桌面控件系统（高度/圆角/字重/禁用/主按钮/工作区压平）

- 编号：DIFF-069
- 影响：A01 外观与全部工具页控件
- 日期：2026-09-15

## 原行为（Electron）

`global.css` 桌面控件系统：

- `:root` / modern：控件高 34px、圆角 9px；hero 36/12；claude 34/10；smartisan 34/7；miui-v5 34/4
- 字重 regular/medium/semibold 为 500/600/650
- `button:disabled` / `select:disabled` 透明度 0.42
- 主操作 `.primary-command` 使用 `--desktop-button-prominent`（modern 为中性深色，不是强调色）
- `.tool-page--workspace` 的 JSON/HTTP 在 modern/quiet/miui 下去掉 editor-shell 圆角与阴影；hero/smartisan/claude 再加回
- `.tool-page h1` / `.settings-content__header h1` 用 `--text-strong` 与 semibold；设置标题 22px
- `.status-pill` 为 12px medium

## 本产品行为

- `MooDimens` 默认 34/9/工具栏 48；`resolveMooDimens` 按风格覆盖
- `MooButton` / 开关禁用整控件 alpha 0.42；按钮 12sp SemiBold
- 主按钮走 `prominent`（`--desktop-button-prominent`：modern 中性深色；hero/smartisan 用 accentAction；miui/claude 用风格强调色）
- Tab/开关/「更多」仍用 `primary`，对照 `.toolbar-button--primary`（control 底 + `--text-strong`），不是强调色也不是 prominent
- 图标按钮对照 `.toolbar-button` / `.icon-button`：默认透明边、无阴影
- 工具页标题 `MooPageTitle`（16sp SemiBold）；设置分类头 22sp
- JSON Vault/编辑器/检查器与 HTTP 集合/编辑器在非 hero/claude/smartisan 时 `flatten`：无圆角无外阴影，仅右侧分隔线（检查器无右边线）
- Git 仓库摘要用 `MooStatusPill`；确认删除/丢弃用 `danger`（对照 `.dialog-button--danger`）
- 二维码预览在 modern/quiet/miui 下去掉圆角，对照 workspace flatten

## 理由

此前控件高 32、圆角 8，主按钮用强调色，JSON 工作区始终带壳，与 Electron 桌面控件系统不一致。

## 证据

`ThemeContrastTest` 的 dimens / prominent token 断言。`desktopTest` 见 `docs/acceptance.md`。

## 未做

- 仍不是六套 CSS 逐选择器移植
- 系统 IME、托盘 TCC、三平台安装仍未测
- 产品窗其余 Tab 走查仍缺
