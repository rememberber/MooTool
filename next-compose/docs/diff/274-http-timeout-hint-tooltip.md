# DIFF-274：HTTP 超时输入 tooltip 文案

## 对照 Electron

`HttpTool` 超时控件 `title={t('http.timeoutHint')}`，说明 1000–120000 ms 范围。

## 行为

- i18n：`http.timeoutHint`（中/英）。
- `HttpScreen` 超时 `MooTextField` 外包 `MooTooltip`。

## 验证

- `HttpTimeoutHintI18nTest`
- `./gradlew :composeApp:desktopTest --offline`
