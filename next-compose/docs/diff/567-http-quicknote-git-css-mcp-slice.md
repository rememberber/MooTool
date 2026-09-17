# DIFF-567：F09 响应头/状态/CSS + F01 随手记 Git flush Presentation + Vault MCP 双库 notes search offset + 帧 162

## 背景

DIFF-566 已做六套 CSS（diff nav / HTTP 响应头 / 翻译底栏 / 打开安装包 / 随手记 Vault 底栏 / 随机行）、JSON Vault Git flush、`mergeContinueActionEnabled`、HTTP/翻译边界 UI、更新打开安装包、Vault MCP notes search offset/双库 read、Compose 帧 `160`/`161`；本条**不重复** 566 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### 样式（CSS 组件批次，非 566）

- `mooHttpResponseHeadBar` / 增强 `mooHttpResponseHead` / `mooHttpPreviousResponseHead`（F09 `.http-response-pane > header` toolbar + 底边线）。
- `mooHttpResponseCodeEditor`（F09 `.http-response-code-editor` 11×13 内边距）。
- `mooHttpStatusMeta` + `HttpResponsePresentation.statusMetaSuccess`（F09 `.http-status` / `.http-status--ok` 成功/失败色）。

### F01 / Git 产品流

- `QuickNoteVaultFooterPresentation.gitFlushSkipsWhenClean` / `gitUntitledBlockKey`；`quickNoteGitFlushBeforeAction` 改经 Presentation 守卫（对齐 DIFF-566 JSON `JsonVaultFooterPresentation`）。

### Vault MCP stdio（双库 + offset 余量）

- `subprocessDualVaultNotesSearchHonorsOffsetWhenJsonGranted`：双库 access 同会话 notes search offset/limit（**非** 566 单库 notes offset / 562 双库 search offset=0 链）。

### 证据脚本

- `mootool_evidence_print_http_response_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F09 Compose 帧 `162` 提示。

### Compose 证据

- `HttpResponseHeadCaptureTest` → `162-compose-http-response-head-tab-focus.png`（非产品主窗）。

## 验证

- `QuickNoteVaultFooterPresentationTest` / `HttpResponsePresentationTest` / `HttpResponseHeadCaptureTest` / `AiIntegrationVaultMcpConnectionTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
