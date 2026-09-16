# DIFF-072：输入框表面与悬停边

- 编号：DIFF-072
- 影响：A01 外观（文本输入）
- 日期：2026-09-15

## 原行为（Electron）

桌面输入默认 `background: var(--surface)`；悬停边为 `--desktop-control-border-hover`。smartisan/miui 另加 inset 阴影，焦点边走强调色。

## 本产品行为

- `MooTextField` 底改为 `workspace`（对照 `--surface`），不再一律 `inset`
- 悬停边 `borderControlHover`
- smartisan/miui 顶部画 inset 低光带
- modern/quiet 未悬停 `MooButton` 边为透明，对照 `.toolbar-button { border: 1px solid transparent }`

## 理由

此前所有风格输入都是凹陷 inset 底，modern/hero/claude 与 Electron 白底字段不一致。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、三平台安装仍未测。
