# DIFF-583：F23 导出/批处理失败 toast + 图库 footer CSS + 帧 178

基线：DIFF-582（工作区）。

## 范围

- **F23**：`ImageWiringPresentation.runExportAssets` + `shouldToastProcessFailure`；导出、重命名、保存、导入、Base64 解码与压缩/水印批处理失败 error toast（取消除外）。
- **CSS**：`mooImageLibraryFooterActions`（Electron `.image-library footer` 40px）。
- **证据**：`ImageLibraryExportCaptureTest` → `178-compose-image-library-export-tab-focus.png`。

**不重复** 582：F24 PDF 合并 toast / SVG `runWriteSvgFile` / 帧 177。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
