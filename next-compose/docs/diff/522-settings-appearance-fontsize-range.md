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

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 联网大走查与 multipart 文件上传、PDF 加密/UI 走查、托盘权限对话框手工验收；HTTP/PDF/Protobuf/Crypto fixture 与更新调度见 [DIFF-523](523-http-pdf-crypto-tray-update-slice.md)。
