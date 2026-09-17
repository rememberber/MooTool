# DIFF-554：F20/F24/F07 引擎接线 + Vault Git remote + 列编辑 notice + CSS + MCP + 命令盘

## 背景

DIFF-553 已做 F05/F11/F23 接线、`JsonVaultSearchPresentation`、`SettingsRowPresentation` 与列编辑证据 hint；本条**不重复** 553 的 CodeRun/Net/ImageSvg/JSON Vault 搜索/设置行链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial F 工具缺口、目标未达成。

## 行为

### F24 / F07 / F20 引擎（非 metadata）

- `PdfWiringPresentation`：任务上限、添加/拆分/合并按钮守卫；`PdfScreen` 接线。
- `ProtobufWiringPresentation`：各 Tab 操作启用与复制载荷；`ProtobufScreen` 接线。
- `TranslationWiringPresentation.languagePairFromSettings`：设置语言对规范化（Screen 仍经既有 `buildInput`）。

### Vault Git remote

- `GitVaultRemotePresentation`：未保存 remote 草稿检测；fetch/pull/push 仍只认已持久化 `status.remote`；面板 hint `git.remoteUnsavedHint`。

### 列编辑（EditorHost 语义）

- `EditorColumnEditPresentation.columnNoticeKey`：JSON/随手记闩锁 + wrap 提示键集中（非 IME 手工 PNG）。

### 样式（CSS 组件批次）

- `mooPdfToolbarActions` / `mooPdfEmptyState` / `mooProtobufConvertGrid` / `mooTranslationAutoRow`

### A02 / MCP / 命令盘

- MCP `mootool_protobuf_wire`（hex/base64 wire 解码，对齐 F07 wire Tab）。
- `CommandSearchCatalog`：`protobuf_wire`/`fetch`/`protobuf`/`pdf split` 等关键词；`ToolRegistry` F20 增 google/bing/debounce。

## 验证

- `PdfWiringPresentationTest` / `ProtobufWiringPresentationTest` / `GitVaultRemotePresentationTest` / `TranslationWiringPresentationTest` / `EditorColumnEditPresentationTest`
- `CommandSearchCatalogTest`（protobuf_wire / fetch）/ `MooToolMcpToolsTest.protobufWireDecodesHexViaMcp`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗全工具 Tab/系统 IME 手工 PNG、P7 三平台安装/公证、其余 F 工具 substantial 引擎/UI、目标未达成。
