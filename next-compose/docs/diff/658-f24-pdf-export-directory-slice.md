# DIFF-658：F24 PDF IO `tools.exportDirectory`

基线：DIFF-657（工作区）。

## 范围

- **F24**：添加 PDF（多选）与合并另存统一 `chooseFilesWithExportDirectory` / `chooseFileWithExportDirectory` + 选文件后 `persistToolsExportDirectory`（移除本地 `choosePdfs`/`chooseSave` 裸 `FileDialog`）。
- **共享**：`ToolsExportDirectory.chooseFilesWithExportDirectory` 多选 LOAD 辅助。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
