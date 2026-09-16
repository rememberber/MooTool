# DIFF-185：Java `func.crypto` 摘要文件路径与随机长度会话

## 背景

Java `config.setting` 在 `func.crypto` 组保存 `digestFilePath`、`randomNumDigit`、`randomPasswordDigit`（另有 `randomStringDigit` 已写入 `AppSettings.tools.randomStringLength`）。Compose 加解密工具使用单一 `CryptoSession.randomLength` 驱动数字/字符串/密码随机长度，摘要 Tab 仅持久化 `digestFileName` 与 `digestOutput`。

## 行为

- 迁移确认后 `LegacyJavaSettings.applySessionPatches`：
  - 若 `digestFilePath` 指向本机常规文件：写入 `digestFileName`，并按当前会话摘要算法计算 `digestOutput`（与用户在 Java 版选文件后点摘要一致，算法以 Compose 会话默认为准）。
  - 若存在 `randomStringDigit` / `randomNumDigit` / `randomPasswordDigit` 任一键：取三者数值的 **最大值** 写入 `randomLength` 并持久化（Compose 单字段；避免某一类随机长度低于 Java 配置）。
- `hasMigratableSettings` 识别上述键。

## 证据

- `LegacyJavaSettingsTest.applySessionPatchesRestoresCryptoDigestFileAndRandomLengths`
