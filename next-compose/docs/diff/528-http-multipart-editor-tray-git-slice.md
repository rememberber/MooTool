# DIFF-528：multipart 构建 + JSON 全局 softWrap + 托盘 TCC 证据 + Vault Git discard

## 背景

DIFF-527「未做」：Electron/Compose 均无 multipart **文件** Tab（`networkService.buildRequestBody` 仅 body 字符串）；托盘 TCC **手工**验收、设置/编辑器缺口、Vault Git 走查。本切片补引擎级 multipart 构建与可选 httpbin POST、F04 JSON 全局 softWrap 即时同步、托盘 TCC 产品证据脚本（恢复基线 PNG）、合并冲突期 discard 单测。

## 行为

### F09 HTTP

- `HttpMultipartPart` / `HttpEngine.buildMultipartFormData` / `multipartContentType`：程序化 text+filename 字段，写入 Body 编辑器 + `Content-Type` 后与 [DIFF-527](527-tray-permission-pdf-outline-http-slice.md) `prepare` 原样发送一致。
- `HttpEngineTest.buildMultipartFormDataIncludesTextAndFileParts`；可选 `MOOTOOL_HTTP_MULTIPART_SMOKE=1` → `optionalHttpBinMultipartPostSmoke`（httpbin/localhost 白名单，默认 CI 跳过）。

### A01 设置 · 编辑器（F04 JSON）

- `EditorSettingsLiveApply` + `JsonScreen` `LaunchedEffect(settings.editor.softWrap)`：JSON 主编辑器 wrap 跟随全局设置（随手记仍用笔记 metadata `lineWrap`）。

### A03 托盘 / F22 / F23 TCC 证据

- `scripts/prepare-tray-screencapture-evidence.sh`：登记手工步骤；从 `57-color.png` 恢复 `docs/evidence/2026-09-17-tray-tcc-screencapture/reference/57-color-baseline.png`。
- `product-evidence-common.sh`：`mootool_evidence_print_tray_tcc_hint` / `mootool_evidence_print_http_multipart_smoke_hint`；`verify-product-evidence-prep.sh` 与 `ProductEvidencePrepScriptTest` 覆盖。

### F01/F04 Vault Git

- `GitDiscardDuringMergeTest.discardRestoresTrackedConflictFileDuringMerge`：merge 冲突期 discard 单文件（对齐 Electron `VaultGitService.discard`）。

## 验证

- `HttpEngineTest.buildMultipartFormDataIncludesTextAndFileParts`
- `EditorSettingsLiveApplyTest`
- `GitDiscardDuringMergeTest`
- `ProductEvidencePrepScriptTest.prepareTrayScreencaptureScriptRestoresBaselinePng`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗全 Tab 走查、multipart 文件 Tab UI（Electron 亦无）、TCC 系统对话框 PNG 手工验收、P7 三平台安装/公证/升级卸载、非对称 Tab 逐控件走查、PDF 跨文档书签完整策略。
