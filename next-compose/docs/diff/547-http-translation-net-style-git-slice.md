# DIFF-547：F09/F20 在途响应呈现 + CSS 批次 + Git 产品证据 + 命令盘

## 背景

DIFF-546 已做 HTTP「上次响应」、翻译 debounce/restore 抑制、PDF/HTTP 请求 pane 样式；本条**不重复** 546 的 `mooHttpRequestPane`/`mooPdfTableWrap`/`mooTranslationHistoryArticle` 或 F19 会话模块。parity-gap 仍列 F09/F20 过期响应丢弃可单测化、Electron `.http-entry-row`/`.http-saved-item`/`.http-response-pane`/`.translation-editor-grid`/`.net-command-row` 样式、Vault Git merge 产品窗脚本提示、设置/网络命令盘关键词。

## 行为

### F09 HTTP

- `HttpRequestPresentation`：`shouldApplyResponse`、`previousResponseBeforeSend`、`canSend`；Screen 发送回调与 stash 上一响应改用 domain（仍走 OkHttp 真请求，无 mock）。
- `mooHttpSavedItem` / `mooHttpEntryRow` / `mooHttpResponsePane` 用于集合列表、Params 表、响应区。

### F20 翻译

- `TranslationResponsePresentation`：序号 + `requestId` 丢弃过期结果、debounce seq 守卫、`ABORTED` 不写 error；Screen 发送/自动翻译改用 domain（仍走 `TranslationEngine` 真联网）。

### F11 网络

- `mooNetCommandRow` 用于 PING/扫描等命令行最小高度（对齐 `.net-command-row` dense）。

### A01 / A03

- 命令盘 `network` 增 `httpbin`/`response`/`cookie`/`header`；`tools` 增 `debounce`/`provider`/`500`。
- `mootool_evidence_print_git_merge_product_hint` + `prepare-git-merge-conflict-evidence.sh` 输出 §B 走查；`ProductEvidencePrepScriptTest` 断言 Vault 冲突随手记样本。

### 样式（CSS 组件批次）

- `mooTranslationEditorSeam`：翻译双列中缝（`.translation-editor-grid`）。

## 验证

- `HttpRequestPresentationTest` / `TranslationResponsePresentationTest` / `CommandSearchCatalogTest` / `ProductEvidencePrepScriptTest`
- `./scripts/verify-product-evidence-prep.sh`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、真实 Google/Bing/公网 HTTP 大走查、产品主窗 Vault/IME PNG、P7 三平台安装、目标未达成。
