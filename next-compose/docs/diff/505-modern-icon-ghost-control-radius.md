# DIFF-505：`MooGhostButton` 圆角（`.icon-ghost`）

## 背景

DIFF-504 将 **`MooIconButton`** 对齐 Electron **`.icon-button`**（modern 9dp、`controlHeight` 方钮、浅阴影）。侧栏 ⌕/折叠/分组、命令盘关闭、查找条导航、toast 关闭等走 **`.icon-ghost`**（30×30 透明底，见 [DIFF-076](076-style-toast-http-tabs-ghost.md)）。此前 `MooGhostButton` 固定 **6dp** 圆角，与 Electron 默认 **8px** 及 modern/hero 风格块不一致。

parity-gap **P1 外观 / 工作台侧栏**；延续 DIFF-504 控件半径链，不展开设置未实现分类、Git/Vault 新切片。

## 行为

- **`LayoutPolicy.iconGhostCornerRadiusDp`**：modern 为 **9dp**（与 `--desktop-control-radius` 同 `iconButtonCornerRadiusDp`）；hero **12dp**；其余风格（含 quiet/claude/smartisan/miui）为 **8dp**（对齐 `global.css` 中 `.icon-ghost { border-radius: 8px }`，quiet 下 ghost 仍为 8px 而非 6dp 的 `.icon-button`）。
- **`MooGhostButton`**：圆角走上述策略；默认 **30dp** 尺寸与 hover 透明→`hoveredControlFill` 不变；无 modern 浅阴影（Electron 未给 `.icon-ghost` 加 shadow）。

## 验证

- `LayoutPolicyTest.iconGhostCornerRadiusMatchesElectronIconGhostNotIconButtonOnQuiet`
- `GhostButtonModernCaptureTest` → `docs/evidence/2026-09-15-tray-density/captures/icon-ghost-modern-search.png`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具走查、设置未实现分类面板、F-tool 行为新切片、Git/Vault、P7 三平台安装验收、hero/claude 内容区 shell 走查、`.icon-ghost` hover 在 quiet 下 `control-active` 与 classic `control-hover` 的细差。
