# DIFF-558：设置关于/更新 UI + Vault 外部冲突 UI + MCP protobuf_wire 集成

## 背景

DIFF-557 已做列编辑语义、F08/F25/F11 引擎 UI、JSON 检查器、Vault Git merge 产品 UI 与 env/hardware CSS；本条**不重复** 557 接线/列选/env/hardware 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### 设置 · 关于与更新（对齐 Electron `.settings-update-result`）

- `UpdateAboutPresentation` 扩展：`resultHeadlineKey`、目标平台行、缺包提示、错误行、`showUpdateResultSection`。
- `SettingsAboutPanel` / `SettingsUpdateResultCard`：从 `SettingsScreen` 抽出；`mooSettingsAboutHero` / `mooSettingsUpdateResult` / `mooSettingsUpdateResultNotes` / `mooSettingsUpdateResultFile` CSS 批次。
- i18n：`settings.update.target` / `noDownload` / `resultUpToDate`。

### Vault 外部冲突 UI

- `VaultConflictPresentation`：`saveCopyProminent` / `reloadProminent` / `showDiffPreview`；删除态另存副本为主按钮。
- `VaultConflictDialog`：`mooVaultConflictPathRow` / `mooVaultConflictDiffPreview` / `mooVaultConflictActions`。

### A02

- `CommandSearchCatalog`：`external` / `savecopy` / `vaultconflict` / `reload`→Vault。

### MCP

- `AiIntegrationMcpProtobufWireTest`：stdio 子进程调用 `mootool_protobuf_wire`（hex）。

### Compose 证据

- `SettingsAboutCaptureTest` → `152-compose-settings-about-update-check-tab-focus.png`。

## 验证

- `UpdateAboutPresentationTest` / `VaultConflictPresentationTest` / `CommandSearchCatalogTest` / `SettingsAboutCaptureTest` / `AiIntegrationMcpProtobufWireTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault 外部冲突/IME 手工 PNG、P7 三平台安装/公证、其余 substantial 引擎/UI、目标未达成。
