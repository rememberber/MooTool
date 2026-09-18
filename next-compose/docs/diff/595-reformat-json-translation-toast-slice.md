# DIFF-595：F03/F04/F20 失败 toast 呈现层 + 帧 190

基线：DIFF-594（工作区）。

## 范围

- **F03**：`ReformatWiringPresentation.shouldToastIoFailure` + `notifyReformatFailure`（格式化/读源/写结果失败）。
- **F04**：`JsonWiringPresentation.shouldToastTransformFailure` / `shouldToastIoFailure` + `notifyJsonFailure`（导入/导出/转换/JSONPath/检查器路径查询失败）。
- **F20**：`TranslationWiringPresentation.shouldToastTranslateFailure` / `shouldToastSaveFailure` + `notifyTranslationFailure`（翻译响应失败、存单词失败）。
- **证据**：`ReformatFormatTabCaptureTest` → `190-compose-reformat-format-tab-focus.png`。

**不重复** 594：F02/F10/F21 / 帧 189。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
