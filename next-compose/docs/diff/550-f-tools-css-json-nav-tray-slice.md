# DIFF-550：余下 F 工具 metadata + CSS 批次 + JSON 检查器 + 导航/更新·托盘关键词

## 背景

DIFF-549 已做 F13/F09/F14 **metadata**、F17/F14 QR/随机接线与 encode/qr/env/hardware/net CSS；本条**不重复** 549 的 `QrWiringPresentation`/`RandomWiringPresentation` 或 encode/qr/random 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装/公证、真实联网大走查、目标未达成。

## 行为

### F 工具 metadata / 历史 options

- `ImageHistoryMetadataTest`、`PdfHistoryMetadataTest`、`HostHistoryMetadataTest`、`UaHistoryMetadataTest`、`RegexHistoryMetadataTest`
- `ConfigHistoryMetadata` + `CalculatorHistoryMetadata`；`ConfigHistoryRestore` 显式 `TO_YAML` 分支
- `MessageBoardSessionMetadataTest`

### JSON 检查器

- `JsonInspectorPresentation`：Schema 推断按钮与重复键路径显隐；Screen 仍经 `jsonInspectorInferSchemaEnabled` 委托

### 样式（CSS 组件批次）

- `mooColorHexColumn` / `mooColorPreviewPane` / `mooCalculatorResultRow` / `mooHostEditBar` / `mooHostProfileSearch`
- `mooTimeCurrentBand` / `mooMessagePresetChip` / `mooRegexTestPane` / `mooUaResultCell` / `mooProtobufWirePane`

### A01 / A02 / A03 / MCP

- `ToolRegistry` 与 `CommandSearchCatalog` 增 color/image/calc/host/ua/regex/pdf/protobuf/message/time/vault structure、general 托盘截图、about 自动下载等关键词
- `MooToolMcpToolsTest.diffToolReportsLineChangesViaMcp`

## 验证

- 上述 metadata/presentation/catalog/MCP 单测
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault/IME/TCC 系统对话框 PNG、P7 三平台安装/公证、F 工具引擎/UI  substantial 缺口、目标未达成。
