# DIFF-181：Java 配置会话补丁 + 时间 ConsoleUtil 解析

## 行为

- **A03**：`config.setting` 中 `func.regex.regexText`、`func.calculator.calculatorInputExpress` 在迁移确认后写入 `RegexSession.pattern` / `CalculatorSession.expression`（`LegacyJavaSettings.applySessionPatches`，在 `t_func_content` 草稿之前执行，草稿仍可覆盖）。
- **A03**：`LegacyTimeConvertDraft.parse` 先经 `LegacyConsoleDraft` 去时间戳，再解析转换行（与计算器/二维码 ConsoleUtil 草稿一致）。

## 验证

- `LegacyJavaSettingsTest.applySessionPatchesRegexPatternAndCalculatorExpression`
- `LegacyTimeConvertDraftTest.parseStripsJavaConsoleUtilTimestamps`
- `./gradlew :composeApp:desktopTest --offline`（**338/338**）
