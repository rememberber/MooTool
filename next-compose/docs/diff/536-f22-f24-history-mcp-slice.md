# DIFF-536：PDF/图片/调色板历史 metadata + F09/F10/F21 恢复 + MCP 缺口

## 背景

DIFF-535「未做」仍列 PDF/图片/调色板历史 options 散落在 Screen、MCP 与 Electron `server.test.ts` 仍有语义差、F08/F25 无通用历史（Electron 亦无）、其余带 `HistoryBrowser` 的工具缺可单测 restore 模块。

## 行为

### F22 调色板

- `ColorHistoryMetadata` / `ColorHistoryRestore`：JSON `options`（格式 + 操作 wire），兼容旧 plain `ColorFormat`/`ColorOperation`/`swap`；恢复主/辅色对与格式，不写回历史。

### F23 图片 / F24 PDF

- `ImageHistoryMetadata`（`process`/`svg`）+ `ImageHistoryRestore`：输出路径与库内资源名解析。
- `PdfHistoryMetadata`（`split`/`merge`）+ `PdfHistoryRestore`：恢复 `lastOutputs`。

### F09 HTTP / F10 配置 / F21 计算器

- `HttpHistoryMetadata`：`operation`=方法、`options`=状态码 JSON；`HttpHistoryRestore`；兼容 `operation` 误存状态码时从 summary 解析方法。
- `ConfigHistoryRestore`：自 Screen 提取 convert/validate/format 分支。
- `CalculatorHistoryRestore`：表达式与结果。

### MCP

- `mootool_json_format` sortKeys+spaces:0 成功路径单测（对齐 `server.test.ts` 首条）。
- `mootool_timestamp` **to-local** 在 MCP 层对 13+ 位数字按毫秒解释（对齐 Electron `timeTools.ts`；F18 UI 仍显式单位，见 DIFF-001）。

### F08 / F25

- 对照 Electron：环境变量与系统信息工具**无**通用 `HistoryBrowser`；Compose 仍仅会话 metadata/restore（DIFF-535），不新增通用历史。

## Fixture

- `docs/fixtures/electron-next-colorPdfImage-history-vitest.md`
- `docs/fixtures/electron-next-mcp-server-vitest.md` 登记 MCP json_format / 13 位 to-local

## 验证

- `ColorHistoryMetadataTest` / `ColorHistoryRestoreTest` / `ImageHistoryRestoreTest` / `PdfHistoryRestoreTest` / `HttpHistoryRestoreTest` / `ConfigHistoryRestoreTest` / `MooToolMcpToolsTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**859/859** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 861 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、Host/UA/格式化/运行台等历史 metadata 提取、Vault Git UI 大改、目标未达成。
