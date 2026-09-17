# DIFF-556：余下 F 工具引擎接线 + 随手记 Vault 底栏 + CSS 批次 + 导航关键词

## 背景

DIFF-555 已做 F10/F13/F14/F19 引擎接线、JSON 列编辑 wrap notice、`JsonVaultFooterPresentation`、更新/托盘与 crypto/vault CSS；本条**不重复** 555 的 Crypto/Encode/Host/MessageBoard、JSON 列编辑、Json 底栏、Update/Tray 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial F 工具缺口、目标未达成。

## 行为

### F22/F21/F06/F12/F15/F18/F23/F02/F16 引擎（非 metadata）

- `ColorWiringPresentation` / `CalculatorWiringPresentation` / `ConfigWiringPresentation` / `UaWiringPresentation` / `RegexWiringPresentation` / `TimeWiringPresentation` / `ImageWiringPresentation`：工具栏操作启用守卫；对应 Screen 接线。
- `TextDiffPresentation` 扩展：`canNavigateDiffs` / `canManualCompare`；`TextDiffScreen` 接线。
- `CronWiringPresentation` 扩展：`canParse` / `canCopyRuns`；`CronScreen` 接线。

### F01 Vault

- `QuickNoteVaultFooterPresentation`：底栏路径/脏标记/复制守卫（对齐 JSON）；`QuickNoteScreen` 底栏接线。

### 样式（CSS 组件批次）

- `mooQuickNoteVaultFooter` / `mooColorFormatRow` / `mooUaParseBar` / `mooConfigTabsRow` / `mooDiffNavCluster`

### A02 / MCP

- `CommandSearchCatalog` / `ToolRegistry`：color/calc/config/ua/time/regex/image/diff/cron/quicknote 深链关键词。
- Electron MCP 仍为 11 项核心工具 + Vault 扩展，本条无新增 MCP handler（对照 `docs/fixtures/electron-next-mcp-server-vitest.md`）。

## 验证

- 上述 `*WiringPresentation*` / `QuickNoteVaultFooterPresentation*` / `TextDiffPresentation*` / `CronWiringPresentation*` 单测
- `CommandSearchCatalogTest`（yaml / quicknote / myers）
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗全工具 Tab/系统 IME 手工 PNG、P7 三平台安装/公证、其余 F 工具 substantial 引擎/UI、目标未达成。
