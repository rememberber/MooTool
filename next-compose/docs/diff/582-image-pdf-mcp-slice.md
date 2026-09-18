# DIFF-582：F23 SVG 写出 run* + F24 任务失败 toast + 帧 177

基线：DIFF-581（工作区）。

## 范围

- **F23**：`ImageSvgWiringPresentation.runWriteSvgFile`；矢量化写出失败 `reformat.error.write` + error toast。
- **F24**：拆分/合并失败 `PdfWiringPresentation.shouldToastJobFailure` + `toastError`（取消除外）。
- **证据**：`PdfToolbarActionsCaptureTest` → `177-compose-pdf-toolbar-merge-tab-focus.png`。

**不重复** 581：F17 QR 保存 / notes read→notes search / 帧 176。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
