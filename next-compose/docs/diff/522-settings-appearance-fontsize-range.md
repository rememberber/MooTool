# DIFF-522：设置界面字号 range 即时提交

## 背景

DIFF-521「未做」仍列 **外观** `appearance.fontSize` 用分段（仅 12/13/14/16/18），Electron `SettingsWindow` 为 `type="range"` min 12 max 18，拖动即 `commit`（对齐 `next/src/shared/contracts/settings.ts` `clampNumber(..., 12, 18)`）。

## 行为

- 设置 · 外观：`MooSegmented` 改为 `Slider`（12–18、5 steps → 7 档整数）+ 右侧数值，拖动即 `updateSettings`（非失焦链）。
- 加载/Electron 迁入边界仍由 `SettingsNumericBounds`（[DIFF-485](485-settings-numeric-bounds-normalize.md)）负责；15/17 等中间档可保留。

## 验证

- `SettingsNumericBoundsTest.appearanceFontSize_keepsMidRangeValues`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 联网/二进制大走查、PDF/加解密大切片、托盘/更新手工验收。
