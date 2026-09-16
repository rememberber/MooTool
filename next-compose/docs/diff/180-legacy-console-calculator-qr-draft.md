# DIFF-180：Java ConsoleUtil 草稿（计算器/二维码）

## 行为

- **A03**：Java `Calculator` / `QrCode` 的 `t_func_content` 经 `ConsoleUtil` 写入（空行 + 时间戳 + 消息）。迁入时：
  - `LegacyConsoleDraft` 按块剥离时间戳，保留真实消息行。
  - **计算器**：`LegacyCalculatorDraft` 将消息倒序写入 `log`（与 compose 最新在前一致），并从最后一条解析 `expr = result` 或 `DEC/HEX/BIN(...)` 进制换算字段。
  - **二维码**：从最后一条 `生成:` / `Generate:` 块提取正文写入 `QrSession.content`。
- **A01**：设置导航工具显隐切换后立即 `normalizeHiddenNavigationToolIds`（与 save 规范化一致）。

## 验证

- `LegacyConsoleDraftTest`、`LegacyCalculatorDraftTest`
- `LegacyToolDraftApplierTest.appliesQrCodeDraftFromJavaConsoleLog`
- `./gradlew :composeApp:desktopTest --offline`（**336/336**）
