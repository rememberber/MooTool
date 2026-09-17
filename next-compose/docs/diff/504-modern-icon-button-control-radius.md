# DIFF-504：`MooIconButton` 圆角/尺寸/阴影（`.icon-button`）

## 背景

DIFF-503 将 modern 非 p5 **`MooButton`** 圆角对齐 Electron `.toolbar-button` 的 **7px**。Electron 同风格下 **`.icon-button`** 仍走 `--desktop-control-radius`（modern **9px**），并带 `box-shadow: 0 1px 2px var(--shadow-soft)`（见 `next/src/shared/styles/global.css`）。此前 `MooIconButton` 虽用 `MooDimens.radius`，但未与 toolbar 7dp 策略显式区分；工具页 `.tool-page .icon-button` 为 **34×34**，Compose 最小宽曾为 32dp。

parity-gap **P1 外观 / 工具页内容区**；延续 DIFF-503，避免设置未实现分类链。

## 行为

- **`LayoutPolicy.iconButtonCornerRadiusDp`**：各风格 `--desktop-control-radius`（modern 9、quiet 6、hero 12…），**不是** `nonP5ToolbarCornerRadiusDp` 的 7dp。
- **`LayoutPolicy.iconButtonSoftShadow`**：modern 为 true，对应 `.icon-button` 浅阴影。
- **`MooIconButton`**：圆角/阴影走上述策略；默认 `minWidth`/`minHeight` 均为 `controlHeight`（34dp），对齐 `.tool-page .icon-button`。显式 `28×28`（如 JSON 检查器关闭）仍由调用方 modifier 覆盖。

## 验证

- `LayoutPolicyTest.iconButtonCornerRadiusUsesDesktopControlRadiusNotToolbarSevenDp`
- `IconButtonModernCaptureTest` → `docs/evidence/2026-09-15-tray-density/captures/icon-button-modern-close.png`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具走查、设置未实现分类、Git/Vault 新切片、P7 三平台安装验收、hero/claude 内容区 shell 走查；`MooGhostButton` 与 `.icon-ghost` 见 [DIFF-505](505-modern-icon-ghost-control-radius.md)。
