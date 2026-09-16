# DIFF-071：开关、分段、提示、状态栏与遮罩按风格对齐

- 编号：DIFF-071
- 影响：A01 外观（开关、分段、tooltip、Git 状态胶囊、JSON/随手记状态栏、弹层遮罩）
- 日期：2026-09-15

## 原行为（Electron）

各 `data-interface-style` 块对 `.toggle`、`.segmented__item--active`、`.tooltip`、`.status-pill`、`.json-statusbar`、`.dialog-backdrop` 有不同边框、轨道、拇指与遮罩：

- modern / quiet / hero：`.toggle { border: 0 }`；hero 未选中为 `--surface-card-hover`，选中为 `--accent-strong`
- smartisan：inset 轨道、开为 `--smartisan-red`、拇指 18px raised 渐变；选中分段为 raised + `border-control`；遮罩 `rgba(35,31,27,0.42)`
- miui-v5：矩形 3px 开关、选中分段底栏橙色条、tooltip 4px、遮罩 0.38
- claude：无边开关、纸色拇指、tooltip 8px、胶囊暖底、遮罩 0.34
- tooltip 底/字对齐 `--tooltip-bg` / `--tooltip-text`（modern 浅 `#2E2E31`/`#fff`）
- JSON/随手记状态栏 10px `--text-muted`，底为 toolbar

## 本产品行为

- `tooltipFill` / `tooltipContent` / `tooltipRadius` 与 `overlayScrim` 按风格取值，不给 `MooColors` 加字段
- `MooSwitch` 按风格改尺寸、无边/inset/矩形轨道与拇指渐变
- `MooSegmented` smartisan 选中走 raised，miui 选中底栏橙色
- `MooStatusPill` claude 暖底、smartisan raised、miui 4dp
- JSON/随手记/应用状态栏改 `MooStatusMeta` 10sp muted；状态栏底用 `toolbarBrush`

## 理由

此前开关一律 1dp 边与 20dp 实心拇指，tooltip 用 `textPrimary` 底，smartisan 浅色遮罩只有 0.20，和风格块不一致。

## 证据

`ThemeContrastTest.tooltipAndScrimTokensMatchElectronStyleBlocks`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、三平台安装仍未测。
