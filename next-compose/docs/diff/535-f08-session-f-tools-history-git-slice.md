# DIFF-535：F08/F25 会话 metadata + F 工具历史恢复 + Vault Git 面板

## 背景

DIFF-534 已补 F08 导出分区、F25 会话 Tab 恢复与 Git push 对齐。parity-gap 仍列：F08/F25 缺与 F11/F15 同级的可单测 **metadata/restore** 模块；Encode/Crypto/Protobuf/Diff/Cron 历史 options 散落在 Screen；Vault Git commit/remote/continue 禁用条件未集中到 `GitOperationPresentation`；A02 命令盘缺 history/收藏深链关键词。

## 行为

### F08 / F25（无通用历史）

- `EnvSessionMetadata` / `EnvSessionRestore`：tab/scope/query wire 与 `VariablesSession` 恢复。
- `HardwareSessionMetadata` / `HardwareSessionRestore`：`HardwareSession` 恢复委托 domain（`HardwareSessionRestoreTest` 仍有效）。
- `VariablesSessionRestoreTest` / `EnvSessionMetadataTest`。

### Vault Git UI

- `GitOperationPresentation`：`fetchEnabled`、`commitEnabled`、`configureRemoteEnabled`、`continueOperationEnabled`（对齐 Electron `VaultGitDialog.tsx`）。
- `VaultGitDialog` 提交/远程/继续按钮改用上述 helper。

### F 工具历史 metadata / restore

- F02 `DiffHistoryMetadata` / `DiffHistoryRestore`
- F07 `ProtobufHistoryMetadata`（JSON + 旧 `|` 管道）/ `ProtobufHistoryRestore`
- F14 `CryptoHistoryMetadata` / `CryptoHistoryRestore`
- F16 `CronHistoryRestore`（时区 + fields 拆分）
- F02 编码 `EncodeHistoryMetadata` / `EncodeHistoryRestore`

### A02

- 命令盘 `layout` 分类增 `history` / `favorite` / `历史` / `收藏` 关键词（`CommandSearchCatalogTest`）。

## Fixture

- `docs/fixtures/electron-next-envTools-vitest.md` 登记 F08 会话 metadata。
- `docs/fixtures/electron-next-cronTools-vitest.md` 登记 `CronHistoryRestore`。

## 验证

- 上述 domain/sessions 单测 + `GitOperationPresentationTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**851/851** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 853 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、F08/F25 通用历史（仍无）、PDF/图片/调色板历史 metadata 提取、MCP 新工具面、目标未达成。
