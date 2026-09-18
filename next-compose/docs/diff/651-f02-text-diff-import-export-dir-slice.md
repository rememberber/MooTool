# DIFF-651：F02 文本对比导入 `tools.exportDirectory`

基线：DIFF-650（工作区）。

## 范围

- **F02**：左右导入由裸 `FileDialog` 改为 `chooseFileWithExportDirectory`；导入成功后 `persistToolsExportDirectory`（对齐 F03/F06/F04 工具 IO 链）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
