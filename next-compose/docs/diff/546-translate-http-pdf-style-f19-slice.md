# DIFF-546：F20/F09/F24 呈现层 + CSS 批次 + F19 会话 + 产品证据

## 背景

DIFF-545 已做 JS/TS 格式化、Vault Git merge 冲突 UI 与 editor 命令盘关键词；本条**不重复** Markdown/JS/TS Prettier、merge 冲突 UI 或 Git 装饰链。parity-gap 仍列 F20/F09/F24 引擎/UI 可单测呈现、Electron `.http-request-pane` / `.pdf-table` / `.translation-history-list` 样式批次、F19 会话 restore 域模块、Vault 产品窗证据脚本与 IME 验收文档。

## 行为

### F09 HTTP

- `HttpResponsePresentation`：`上次响应` 显隐、`visibleResponse`/`usableResponse`（从 `HttpEngine` 抽出；Screen 直读 Presentation）。
- 命令盘 `network` 增 `curl`/`pdf`/`multipart`/`https` 关键词。

### F20 翻译

- `TranslationAutoPresentation`：500ms debounce + `restoredSource` 抑制自动重译（对齐 Electron restore）。
- 历史 Tab 行 `mooTranslationHistoryArticle`（对齐 `.translation-history-list article` 卡片底/悬停）。

### F24 PDF

- `PdfImportPresentation`：结构 inspect info toast 参数与加密 PDF toast 守卫。
- `mooPdfTableWrap` / `mooPdfOutputStrip` 用于表区与输出条。

### F19 留言板

- `MessageBoardSessionMetadata` / `MessageBoardSessionRestore`；`MessageBoardSession.restore` 委托。

### 样式（CSS 组件批次）

- `mooHttpRequestPane`：请求 Params/Headers/Cookies/Body 子面板底（`.http-request-pane`）。

### 产品证据 / IME 文档

- `prepare-vault-conflict-evidence.sh` 增随手记 `sample-external.md`；`mootool_evidence_print_vault_conflict_product_hint` 区分 Compose 帧 `147` 与产品主窗 PNG。
- `verify-product-evidence-prep.sh` / `ProductEvidencePrepScriptTest` 断言新样本。
- `docs/evidence/2026-09-16-editor-manual-acceptance/results.md` 登记 DIFF-546 脚本链。

## Fixture

- `docs/fixtures/electron-next-pageRanges-vitest.md`
- `docs/fixtures/electron-next-translationNetwork-vitest.md`

## 验证

- `HttpResponsePresentationTest` / `TranslationAutoPresentationTest` / `PdfImportPresentationTest` / `MessageBoardSessionRestoreTest` / `CommandSearchCatalogTest`
- `./scripts/verify-product-evidence-prep.sh`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、真实 Google/Bing/公网 HTTP 大走查、产品主窗 Vault/IME PNG、P7 三平台安装、JS/TS/Markdown 完整 Prettier、目标未达成。
