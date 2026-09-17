# DIFF-563：F 工具引擎 run* 路径 + CSS 批次 + Vault 冲突/MCP read + 帧 157

## 背景

DIFF-562 已做 Vault MCP stdio 双库 search、F09/F20/F24/F25 `runSend`/`runTranslate`/`inspectFile`/`runCollect`、`mooGitVaultRemoteRow` 与 Compose 帧 `156`；本条**不重复** 562 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### F 工具引擎路径（Presentation → Engine）

- `EncodeWiringPresentation.runConvert`；`EncodeScreen` 转换经 Presentation 调用 `EncodeEngine`。
- `CryptoWiringPresentation.runDigestText`；摘要 Tab 文本 digest 经 Presentation。
- `CronWiringPresentation.runPreview`；「下次运行」经 Presentation 调用 `CronEngine.nextRuns`/`describe`。
- `TextDiffPresentation.runCompare`；F02 对比/清空/交换经 Presentation 调用 `DiffEngine`。
- `EnvWiringPresentation.runSnapshot` / `runPreviewDiff`；F08 刷新与编辑预览 diff。
- `NetWiringPresentation.runLocalAddresses`；F11 本机地址刷新。

### Vault 外部冲突 / JSON 检查器

- `VaultConflictPresentation.buildUnifiedPreview`；冲突对话框 unified diff 经 Presentation。
- `JsonInspectorPresentation.pathCopyEnabled` / `duplicatePathClickEnabled`；结构面板重复键路径守卫。

### Vault MCP stdio（read 分页）

- `AiIntegrationVaultMcpConnectionTest.subprocessReadJsonVaultHonorsOffsetAndLength`：`mootool_json_documents_read` offset/length（**非** 562 双库 search 链）。

### 样式（CSS 组件批次）

- `mooEncodeConvertButton` / `mooJsonInspectorDuplicatePath` / `mooVaultConflictHintRow`。

### Compose 证据

- `VaultConflictCaptureTest` → `157-compose-vault-conflict-savecopy-tab-focus.png`（非产品主窗）。

## 验证

- `EncodeWiringPresentationTest` / `CryptoWiringPresentationTest` / `CronWiringPresentationTest` / `TextDiffPresentationTest` / `EnvWiringPresentationTest` / `NetWiringPresentationTest` / `VaultConflictPresentationTest` / `JsonInspectorPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `VaultConflictCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault Git/IME 手工 PNG、P7 三平台安装/公证、其余 F 工具 substantial 引擎/UI、目标未达成。
