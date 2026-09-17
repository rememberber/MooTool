# DIFF-502：非 p5 `MooButton` modern/quiet 排版（含 editor-toolbar）

## 背景

DIFF-501 已对齐 `p5Toolbar=true` 的工具页按钮。JSON/随手记/Host/代码运行等 **`.editor-toolbar`** 仍用默认 `MooButton`（12sp SemiBold），与 Electron `:root[data-interface-style='modern'] .toolbar-button`（13px、字重 500）不一致。设置分组内 `MooButton` 在 modern 下同样应对齐 500 字重。

parity-gap **P1 外观 / 工具页内容区**；延续 DIFF-501，避免设置加载规范化链。

## 行为

- **`LayoutPolicy.nonP5ToolbarFontSp` / `nonP5ToolbarFontWeightMedium`**：modern/quiet、非 `dense` 时为 13sp / Medium(500)；hero 等仍为 12sp SemiBold。
- **`MooButton`**：`p5Toolbar=false` 时走上述策略；modern/quiet 的 `primary` 增加 1dp 浅阴影（与 p5 primary、`.toolbar-button--primary` 一致）。
- `p5Toolbar` 与紧凑导航 dense 逻辑不变（DIFF-497/501）。

## 验证

- `LayoutPolicyTest.modernNonP5ToolbarTypographyMatchesElectronEditorToolbar`
- `EditorToolbarModernCaptureTest` → `docs/evidence/2026-09-15-tray-density/captures/editor-toolbar-modern-format.png`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

modern 7dp 圆角见 [DIFF-503](503-modern-toolbar-button-7dp-radius.md)；六套 CSS 逐选择器皮肤、产品窗全工具走查、设置未实现分类、Git/Vault 新切片、P7 三平台安装验收、hero/claude 内容区 shell 走查。
