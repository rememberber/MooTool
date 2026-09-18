# DIFF-581：F17 QR 保存 run* + 失败 toast + 预览操作 CSS + Vault MCP notes read→notes search + 帧 176

基线：DIFF-580（工作区，含 F01 随手记 IO）。

## 范围

- **F17**：`QrWiringPresentation.runWritePngFile`；保存失败 `reformat.error.write` + error toast；`mooQrPreviewActions`。
- **Vault MCP stdio**：双库 notes read → notes search body offset（`subprocessDualVaultNotesReadThenNotesSearchHonorsBodyOffset`）。
- **证据**：`QrPreviewActionsCaptureTest` → `176-compose-qr-preview-save-tab-focus.png`。

**不重复** DIFF-580：F01 QuickNote IO / json read→json search / 帧 175。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
