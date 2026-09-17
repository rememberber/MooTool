# DIFF-503：modern 非 p5 `MooButton` 7dp 圆角（`.toolbar-button`）

## 背景

Electron 全局 `--desktop-control-radius` 为 9px，但 `:root[data-interface-style='modern'] .toolbar-button` 单独设为 `border-radius: 7px`（见 `next/src/shared/styles/global.css`）。DIFF-502 已对齐 modern/quiet 非 p5 字号/字重；Compose `MooButton` 非 p5 仍用风格 `MooDimens.radius`（modern 默认 9dp），与 editor-toolbar 按钮视觉不一致。

parity-gap **P1 外观 / 工具页内容区**；延续 DIFF-502，避免设置加载规范化链。

## 行为

- **`LayoutPolicy.nonP5ToolbarCornerRadiusDp`**：modern、非 `dense` 时为 7f；其余风格回退主题 `radius`（quiet 仍 6dp 等）。
- **`MooButton`**：非 p5 时优先走上述圆角；`p5Toolbar` 仍固定 6dp（DIFF-497/501）。

## 验证

- `LayoutPolicyTest.modernNonP5ToolbarCornerRadiusMatchesElectronToolbarButton`
- `EditorToolbarModernCaptureTest` 帧 `docs/evidence/2026-09-15-tray-density/captures/editor-toolbar-modern-format.png`（圆角随策略更新）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具走查、设置未实现分类、Git/Vault 新切片、P7 三平台安装验收、hero/claude 内容区 shell 走查、`MooIconButton` 与 `.icon-button` 圆角对照。
