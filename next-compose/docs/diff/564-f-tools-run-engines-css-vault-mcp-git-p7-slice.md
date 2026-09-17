# DIFF-564：余下 F 工具 run* 引擎路径 + CSS 批次 + Vault MCP list/search + Git merge UI + 帧 158

## 背景

DIFF-563 已做 F13/F14/F16/F02/F08/F11 `runConvert`/`runDigestText`/`runPreview`/`runCompare`/`runSnapshot`/`runPreviewDiff`/`runLocalAddresses`、Vault 冲突 `buildUnifiedPreview`、JSON 检查器路径守卫、MCP stdio `read` offset/length 与 encode/inspector/conflict CSS、Compose 帧 `157`；本条**不重复** 563 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### F 工具引擎路径（Presentation → Engine）

- `CodeRunWiringPresentation.runDetect` / `parseRunArguments` / `runCode`；F05 检测/参数解析/运行经 Presentation。
- `ProtobufWiringPresentation.runJsonToBinary` / `runBinaryToJson` / `runDecodeWire` / `runConvertBinary`。
- `HostWiringPresentation.runReadSystem` / `runBuildApplyPreview` / `runApply`；F10 读系统/应用预览/写入经 Presentation。
- `UaWiringPresentation.runParse`；F12 UA 解析经 Presentation。
- `RegexWiringPresentation.runMatch`（UI 仍经 `RegexWorkerClient` 进程隔离）。
- `QrWiringPresentation.runGeneratePng`；F17 生成经 Presentation。
- `CalculatorWiringPresentation.runEvaluate` / `runConvertBase` / `runGcd` / `runLcm` / `runPermutation` / `runCombination`。
- `ColorWiringPresentation.runFormatColor`；F22 色值格式化经 Presentation。
- `MessageBoardWiringPresentation.runFitFontSize`；F19 展示字号经 Presentation。
- `ImageWiringPresentation.runDecodeDataUrl`；F23 Base64 导入解码经 Presentation。
- `PdfWiringPresentation.runSplit` / `runMerge`；F24 拆分/合并经 Presentation（`inspectFile` 已在 DIFF-562）。

### Vault MCP stdio（list / search 分页）

- `AiIntegrationMcpListToolsTest`：`listTools` 含 `VaultMcpTools.toolNames` 四项（**非** 563 read offset 链）。
- `AiIntegrationVaultMcpConnectionTest.subprocessSearchJsonVaultHonorsSearchOffset`：`mootool_json_documents_search` offset/limit（**非** 562 双库 search 链）。

### Git merge 产品 UI

- `mooGitMergeResolveRow`；`VaultGitDialog` merge 冲突 ours/theirs 行接线。
- `GitMergeResolveCaptureTest` → `158-compose-git-merge-resolve-tab-focus.png`（非产品主窗）。

### 样式（CSS 组件批次）

- `mooRuntimeDetectBar` / `mooHostApplyButton` / `mooQrGenerateButton` / `mooGitMergeResolveRow`。

### P7

- `prepare-p7-package-smoke.sh` 注释登记 DIFF-564 offline gate；三平台安装/公证仍 **未验收**。

## 验证

- `CodeRunWiringPresentationTest` / `ProtobufWiringPresentationTest` / `HostWiringPresentationTest` / `UaRegexTimeWiringPresentationTest` / `QrWiringPresentationTest` / `CalculatorWiringPresentationTest` / `PdfWiringPresentationTest` / `AiIntegrationMcpListToolsTest` / `AiIntegrationVaultMcpConnectionTest` / `GitMergeResolveCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault Git/IME 手工 PNG、P7 三平台安装/公证、Electron substantial UI 全量移植、目标未达成。
