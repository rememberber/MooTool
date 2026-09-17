# DIFF-579：F09 响应另存 run* + F02 导入簇 CSS + Vault MCP 双库 json search→notes search + 帧 174

## 背景

DIFF-578 已做 F04/F06/F03 工具栏·文件 Tab IO 失败 toast + `mooJsonToolbarIoCluster` + Vault MCP 双库 **json read→notes read** + 帧 `173`；本条**不重复** 578 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F09 HTTP 响应另存 run* + 失败可见

- `HttpResponsePresentation.runWriteResponseText`/`runWriteResponseBytes`；`HttpScreen.saveResponse` 文本/二进制写盘经 Presentation（不再裸 `writeText`/`writeBytes`）。
- 写盘失败写 `reformat.error.write` 到状态栏 + **error toast**（578 未改 F09；此前仅 `http.saveFailed` 文案键）。

### 样式（CSS 组件批次，非 578）

- `mooDiffImportCluster`（F02 工具栏左右导入簇 34dp 行高，对齐 F04 `mooJsonToolbarIoCluster`）。

### Vault MCP stdio（双库 + 跨库 search→search）

- `subprocessDualVaultJsonSearchThenNotesSearchHonorsBodyOffset`：双库 access 同会话 **json search** 提取 needle 后再 **notes search** offset（**非** 578 json read→notes read / 576 notes read→json search / 575 json read→notes search 链）。

### 证据脚本

- `mootool_evidence_print_text_diff_import_cluster_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F02 Compose 帧 `174` 提示。

### Compose 证据

- `TextDiffImportClusterCaptureTest` → `174-compose-text-diff-import-cluster-tab-focus.png`（`mooDiffImportCluster`，非产品主窗）。

## 验证

- `HttpResponsePresentationTest` / `TextDiffImportClusterCaptureTest` / `AiIntegrationVaultMcpConnectionTest` / 既有 F02/F09 单测
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F09/F02/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
