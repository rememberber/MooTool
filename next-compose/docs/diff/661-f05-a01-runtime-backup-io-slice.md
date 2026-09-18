# DIFF-661：F05 运行工作目录 / A01 备份 IO

基线：DIFF-660（工作区）。

## 范围

- **F05**：运行选项「工作目录」改用 `DesktopFileDialogs.chooseDirectory`（初始路径为当前 runtime 工作目录，对齐 Electron `chooseDirectory(runOption.workingDirectory)`）。
- **A01**：备份导出 ZIP / 预览·恢复选 ZIP 改用 `chooseFileWithExportDirectory` + 成功后或选文件后 `persistToolsExportDirectory`（移除裸 `FileDialog`）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
