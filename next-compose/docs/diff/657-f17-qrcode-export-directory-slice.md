# DIFF-657：F17 二维码 IO `tools.exportDirectory`

基线：DIFF-656（工作区）。

## 范围

- **F17**：Logo/识图/保存 PNG 统一 `chooseFileWithExportDirectory` + 选文件后 `persistToolsExportDirectory`（移除本地 `chooseImage`/`chooseSave` 裸 `FileDialog`）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
