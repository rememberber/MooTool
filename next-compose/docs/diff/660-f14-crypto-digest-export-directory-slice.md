# DIFF-660：F14 摘要文件 digest IO `tools.exportDirectory`

基线：DIFF-659（工作区）。

## 范围

- **F14**：摘要 Tab「文件 digest」选文件与拖放 digest 统一 `chooseFileWithExportDirectory` + `persistToolsExportDirectory`（移除裸 `FileDialog`）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
