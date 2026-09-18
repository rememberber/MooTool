# DIFF-659：F23 图片 / F09 HTTP 响应 IO `tools.exportDirectory`

基线：DIFF-658（工作区）。

## 范围

- **F23**：导入多选、批量导出目录、SVG 单文件/多目录另存统一 `chooseFilesWithExportDirectory` / `chooseFileWithExportDirectory` + `persistToolsExportDirectory`（移除裸 `FileDialog` 导入/单文件 SVG 另存）。
- **F09**：响应另存（正文二进制/文本 Tab）`chooseFileWithExportDirectory` + 成功后 `persistToolsExportDirectory`（移除裸 `FileDialog`）。
- **共享**：`persistToolsExportDirectory` 在用户选择目录时写入该目录路径（非仅 parent）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
