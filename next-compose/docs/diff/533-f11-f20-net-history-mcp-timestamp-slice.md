# DIFF-533：F11 历史恢复闭环 + F20 历史回填 + MCP 时间 round-trip

## 背景

DIFF-532 已补 F15/F18 历史 metadata；parity-gap 仍列 **F11** 通用历史恢复：`operation`/`options` 曾写入本地化摘要，端口扫描未保存 `portSpec`，恢复时无法对齐 Electron `NetworkAction` 字符串。**F20** 历史回填逻辑在 Screen 内，缺可单测模块且未同步历史 `translatorType` 到设置/状态栏。**MCP** `mootool_timestamp` 需继承 DIFF-532 `TimeEngine.localToTimestamp` 严格 round-trip 拒绝。

## 行为

### F11 网络/IP

- `NetHistoryMetadata`：Electron 风格 wire id（`ping-range`/`port-scan` 等）与端口 `options` JSON。
- `NetHistoryRestore`：恢复输出区、IPv4↔Long、各命令目标与 `portSpec`；兼容旧记录 `options=NetworkAction` 枚举名。
- `NetScreen`：命令完成与转换成功后按 wire id 写历史。

### F20 翻译

- `TranslationHistoryRestore`：历史 Tab 回填源/目标/语言、`restoredSource` 抑制 500ms 自动重译、写入 `providerUsed`；可选把 `translatorType` 同步到 `settings.tools.translationProvider`。
- `TranslationScreen` 改用上述模块。

### MCP

- `MooToolMcpToolsTest.timestampRejectsLocalTimeThatFailsRoundTrip`：`24:00:00` → `to-timestamp` 错误（与 `TimeEngineTest` 一致）。

## 验证

- `NetHistoryMetadataTest` / `NetHistoryRestoreTest` / `TranslationHistoryRestoreTest` / `MooToolMcpToolsTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**837/837** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 839 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、F08/F25 独立历史（仍无通用历史）、Vault Git UI 大改、目标未达成。
