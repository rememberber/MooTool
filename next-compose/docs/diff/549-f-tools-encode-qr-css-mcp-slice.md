# DIFF-549：F 工具 metadata/引擎接线 + encode/qr/random + CSS 批次 + MCP/A01

## 背景

DIFF-548 已做 F02/F14/F16 历史 **restore**、F20 翻译接线与 HTTP/翻译/网络 CSS 批次；本条**不重复** 548 的 `TranslationWiringPresentation` 或同批 HTTP/翻译样式。parity-gap 仍列 F09/F13/F14 **metadata** 单测、F17/F14 设置→引擎可测化、Electron `encode-panes`/`.qrcode-options`/环境变量表/系统信息行/网络分区样式、Vault·JSON·MCP 命令盘关键词、P7 脚本说明（F07 Protobuf restore 已在 DIFF-543）。

## 行为

### F13 / F09 / F14 历史 metadata

- `EncodeHistoryMetadataTest`、`HttpHistoryMetadataTest`、`CryptoHistoryMetadataTest`。

### F17 / F14 引擎接线

- `QrWiringPresentation`：设置默认尺寸/纠错、编辑器尺寸 normalize（含 `360.4`）、生成 clamp；QR 页 LaunchedEffect/生成/尺寸输入改用 domain。
- `RandomWiringPresentation`：随机串长度 settings↔会话 clamp；加解密页 LaunchedEffect 与生成后写回设置。

### 样式（CSS 组件批次）

- `mooEncodeControlColumn` / `mooQrOptionsRow` / `mooHardwareStatRow` / `mooEnvTableHead` / `mooEnvVarRow` / `mooNetSection`

### A01 / MCP

- 命令盘：`vault` 增 validate/format/duplicate/access；`network` 增 whois/resolve/netstat；`runtime`/`data`/`tools`/`ai` 增 hardware/env/unicode/MCP 工具名关键词。
- `MooToolMcpToolsTest.encodeUrlRoundTripViaMcp`（`mootool_encode` url）。

## 验证

- `QrWiringPresentationTest` / `RandomWiringPresentationTest` / metadata 三测 / `CommandSearchCatalogTest` / `MooToolMcpToolsTest.encodeUrlRoundTripViaMcp`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault/IME PNG、P7 三平台安装/公证、真实 Google/Bing/公网大走查、目标未达成。
