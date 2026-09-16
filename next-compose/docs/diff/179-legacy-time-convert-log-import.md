# DIFF-179：Java 时间转换日志草稿迁入

## 行为

- **A03 / F18**：Java `TimeConvert` 的 `t_func_content` 为左侧转换日志（中/英/日 `时间戳/Time/タイムスタンプ` + `-->` 格式）。迁入时：
  - 按行解析为 compose 时间工具历史（`input`/`output`/`zone`/`unit` 与 `TimeHistoryOptions` 一致）。
  - 会话字段取**最后一行**解析结果（与 Java 关闭前最后一次转换一致）。
  - 纯数字单行草稿仍走原逻辑（仅填时间戳）。

## 验证

- `LegacyTimeConvertDraftTest`
- `LegacyToolDraftApplierTest.appliesTimeConvertJavaLogToSessionAndHistory`
- `./gradlew :composeApp:desktopTest --offline`（**332/332**）
