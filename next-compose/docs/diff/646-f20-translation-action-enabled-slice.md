# DIFF-646：F20 翻译工具栏/单词本/历史 `*ActionEnabled`

基线：DIFF-645（工作区）。

## 范围

- **F20**：`TranslationWiringPresentation.translateActionEnabled` / `copyResultActionEnabled` / `saveWordFromSourceActionEnabled` / 单词本删除·应用·重译·保存 / `clearHistoryActionEnabled`；`TranslationScreen` 对应 `enabled` 接线（对齐 Electron `disabled={!target}` / `!source}` / 空历史清空）。
- **单测**：`TranslationWiringPresentationTest.toolbarWordBookAndHistoryActionEnabled`。

**不重复** 645：645 为跨工具查找条；本切片为 F20 翻译 UI。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
