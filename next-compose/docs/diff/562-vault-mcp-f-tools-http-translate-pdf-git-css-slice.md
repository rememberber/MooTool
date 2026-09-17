# DIFF-562：Vault MCP 双库 stdio + F09/F20/F24/F25 引擎路径 + Git remote CSS/帧 156

## 背景

DIFF-561 已做 MCP stdio hash/diff/json_query/timestamp/uuid、F03 `runFormat`、更新下载流单测、env/net/vault 底栏守卫与 Compose 帧 `155`；本条**不重复** 561 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### Vault MCP stdio（扩展）

- `AiIntegrationVaultMcpConnectionTest.subprocessSearchBothVaultKindsInOneAccessPolicy`：同一 `access.json` 同时授权 notes + json，子进程分别 search。

### F 工具引擎路径（Presentation → Engine）

- `TranslationWiringPresentation.runTranslate`；`TranslationScreen` 主翻译与生词重译经 Presentation 调用 `TranslationEngine`。
- `HttpRequestPresentation.runSend`；`HttpScreen.send` 经 Presentation 调用 `HttpEngine.send`。
- `PdfWiringPresentation.inspectFile`；PDF 导入 ingest 经 Presentation 调用 `PdfEngine.inspect`。
- `HardwareWiringPresentation.runCollect`；`HardwareScreen` 刷新经 Presentation 调用 `HardwareEngine.collect`。

### Git / Vault 产品 UI

- `mooGitVaultRemoteRow`；`VaultGitDialog` remote 输入行接线。
- `GitVaultRemoteCaptureTest` → `156-compose-git-vault-remote-save-tab-focus.png`（非产品主窗）。

## 验证

- `AiIntegrationVaultMcpConnectionTest` / `TranslationWiringPresentationTest` / `HttpRequestPresentationTest` / `PdfWiringPresentationTest` / `HardwareWiringPresentationTest` / `GitVaultRemoteCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault Git/IME 手工 PNG、P7 三平台安装/公证、HTTP/翻译/PDF 其余 substantial UI、目标未达成。
