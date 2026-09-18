# DIFF-647：F20 清空历史确认 + F23 图片库 `*ActionEnabled` 接线

基线：DIFF-646（工作区）。

## 范围

- **F20**：`TranslationWiringPresentation.clearHistoryConfirmActionEnabled`；清空历史确认对话框主钮 `enabled`。
- **F23**：`ImageWiringPresentation.*ActionEnabled` 别名；`ImageScreen` 溢出菜单/库底栏/导入/Base64/水印/重命名对话框统一守卫接线。
- **单测**：`TranslationWiringPresentationTest`、`ImageWiringPresentationTest` 扩展。

**不重复** 646：646 为 F20 主工具栏/单词本；本切片为历史确认 + F23 图片。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
