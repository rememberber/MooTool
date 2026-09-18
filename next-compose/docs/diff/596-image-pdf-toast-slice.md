# DIFF-596：F23/F24 失败 toast 呈现层收尾 + 帧 191

基线：DIFF-595（工作区）。

## 范围

- **F23**：批处理压缩/水印与 SVG 矢量化失败统一经 `notifyImageFailure`（含 IO 文案 `pathHint`；取消除外，沿用 `shouldToastProcessFailure`）。
- **F24**：拆分/合并失败经 `notifyPdfJobFailure`；加密 PDF 导入 toast 经 `notifyPdfImportFailure`（沿用 `showEncryptedImportToast`）。
- **证据**：`ImageSvgStartTabCaptureTest` → `191-compose-image-svg-start-tab-focus.png`。

**不重复** 595：F03/F04/F20 / 帧 190。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
