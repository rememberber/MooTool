# DIFF-548：F 工具历史单测 + F20 Google/Bing 接线 + CSS 批次 + 命令盘/P7

## 背景

DIFF-547 已做 F09/F20 在途响应呈现与 `mooHttpSavedItem`/`mooHttpEntryRow`/`mooHttpResponsePane`/`mooTranslationEditorSeam`/`mooNetCommandRow`；本条**不重复** 547 的 `HttpRequestPresentation`/`TranslationResponsePresentation` 或同批 HTTP 响应样式。parity-gap 仍列 F02/F07/F16 历史 restore 单测、F20 设置→引擎接线可测化、Electron `.http-collection`/`.http-url-bar`/`.http-timeout`/`.http-entry-head`/`.translation-toolbar`/`.net-port-scan-row` 样式、JSON Vault/网络命令盘关键词、P7 脚本说明。

## 行为

### F20 翻译

- `TranslationWiringPresentation.buildInput`：语言对规范化、`parseProvider`、超时 `clamp`；Screen 发送/单词本重译改用 domain。
- `TranslationEngineTest`：本机 HttpServer mock **Bing 首选不触 Google**、Google 请求 `sl`/`tl`/`client=gtx` 接线（离线安全）。

### F02 / F07 / F16 历史

- `DiffHistoryRestoreTest`、`CryptoHistoryRestoreTest`、`CronHistoryRestoreTest`。

### 样式（CSS 组件批次）

- `mooHttpCollection` / `mooHttpCollectionHeader` / `mooHttpCollectionFooter`
- `mooHttpUrlBar` / `mooHttpTimeoutChip` / `mooHttpEntryHead`
- `mooTranslationLangBar`
- `mooNetPortScanRow`

### A01 / P7

- 命令盘 `network` 增 `ping`/`port`/`scan`/`dns`/`traceroute`；`vault`/`editor` 增 `jsonpath`/`search`/`schema` 等。
- `prepare-p7-package-smoke.sh` 注释 DIFF-548 offline gate 范围。

## 验证

- `TranslationWiringPresentationTest` / `TranslationEngineTest` / `DiffHistoryRestoreTest` / `CryptoHistoryRestoreTest` / `CronHistoryRestoreTest` / `CommandSearchCatalogTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、真实 Google/Bing/公网大走查、产品主窗 Vault/IME PNG、P7 三平台安装/公证、目标未达成。
