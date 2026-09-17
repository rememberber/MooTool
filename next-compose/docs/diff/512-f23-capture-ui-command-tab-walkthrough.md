# DIFF-512：F23 区域截图选区 UI、命令盘 Tab 走查单测

## 背景

DIFF-511 已对齐 `ImageEngine.moveCaptureRect`/`resizeCaptureRect` 几何与 Electron `captureSelection.test.ts`，但 `ScreenRegionPicker` 仍在拖选释放后立即裁剪，缺少 Electron `ScreenCaptureOverlay` 的二次调整（移动/角点缩放、Enter/按钮确认）。DIFF-510「未做」仍列命令盘每条结果 Tab 遍历单测。本条接线 **F23 区域截图 overlay 交互** 并补 **A02 命令盘 Tab 走查** 纯函数单测；不重复 P7 分发构建、设置逐控件像素差或产品窗冲突/IME 手工验收。

## 行为

### F23 图片

- `ScreenCaptureInteraction`：屏幕坐标命中（确认/取消钮、四角 handle、选区内移动、区外新建）、拖动手势委托 `ImageEngine` 几何。
- `ScreenRegionPicker`：框选后保留 overlay；可 move/resize；**Enter** 或 ✓ 确认、**Esc**/右键/× 取消；提示文案对齐 Electron `image.captureOverlayHint` / `image.captureOverlayKeys`（zh/en）。
- 图片页与托盘区域截图均传入 `container::t` 以解析 i18n。

### A02 命令盘

- `commandPaletteTabForwardWalkthrough` / `commandPaletteTabBackwardWalkthrough`：从搜索框 Tab 到关闭钮再到选中结果行，以及 Shift+Tab 回到搜索框的完整链。
- `CommandPaletteKeysTest.tabFocusWalkthrough_searchCloseResultRoundTrip` 锁定走查顺序（与 [DIFF-492](492-command-palette-close-tab-focus.md) 一致）。

## 验证

- `ScreenCaptureInteractionTest`
- `CommandPaletteKeysTest.tabFocusWalkthrough_searchCloseResultRoundTrip`
- `./gradlew :composeApp:verifyNativePackageMetadata :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具 Tab 走查帧、P7 三平台安装/公证、设置行内控件与 Electron 像素级差、其余 F-tool 引擎大切片、区域截图多屏/TCC 对话框手工验收、Vault 冲突/Git merge/IME 产品窗证据执行。
