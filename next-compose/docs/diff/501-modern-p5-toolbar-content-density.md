# DIFF-501：modern/quiet 工具页 p5 工具栏内容区密度

## 背景

Electron `.tool-page .toolbar-button` 使用全局 `--tool-control-height: 34px`；`data-interface-style='modern'` 下字号 13px、字重 500，选中态 `--primary` 按钮带 `border-control` 与浅阴影（见 `next/src/shared/styles/global.css`）。Compose `MooButton(p5Toolbar=true)` 此前固定 30dp / 12sp SemiBold，与 `MooDimens.controlHeight`（modern 34dp）及 modern 工具页排版不一致；紧凑导航 dense 26dp 保持不变（DIFF-497）。

parity-gap **P1 外观 / 工具页内容区**；避免继续只做设置加载规范化链。

## 行为

- **`LayoutPolicy.p5ToolbarButtonHeightDp`**：非 dense 时跟当前风格 `controlHeight`；dense 仍为 26dp。
- **`LayoutPolicy.p5ToolbarFontSp` / `p5ToolbarFontWeightMedium`**：modern/quiet 非 dense 为 13sp / Medium(500)；其余风格非 dense 仍为 12sp SemiBold。
- **`MooButton`**：p5 高度/字号/字重走上述策略；modern/quiet 的 p5 `primary` 保留 1dp 阴影（对齐 `.toolbar-button--primary`）。

## 验证

- `LayoutPolicyTest.modernP5ToolbarContentDensityMatchesElectronToolPage`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗 p5 走查帧、P7 三平台安装验收、非 p5 工具栏字号全面对照。
