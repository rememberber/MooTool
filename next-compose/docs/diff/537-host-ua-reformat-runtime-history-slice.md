# DIFF-537：Host/UA/格式化/运行台历史 metadata + restore

## 背景

DIFF-536「未做」仍列 Host/UA/格式化/运行台等历史 options 散落在 Screen；带 `HistoryBrowser` 的工具需可单测 restore 模块以对齐 Electron 写入语义。

## 行为

### F10 Host

- `HostHistoryMetadata` / `HostHistoryRestore`：`operation`=apply、`options`=备份路径；恢复仅写回编辑区正文（对齐 Electron 无通用历史 UI，Compose 保留历史时恢复行为与 JSON/Host 一致）。

### F12 UA

- `UaHistoryMetadata`（`UaParse` marker）/ `UaHistoryRestore`：恢复 source + 解析结果 JSON。

### F03 格式化

- `ReformatHistoryMetadata`（`|` 管道，含 file 文件名）/ `ReformatHistoryRestore`：自 Screen 提取，对齐 `ReformatTool.tsx` `extraData`。

### F05 代码运行

- `CodeRunHistoryMetadata`（JSON arguments/workingDirectory/runtime，对齐 Electron `runOption`）/ `CodeRunHistoryRestore`：恢复源码与 runtime Tab；历史 apply 仍不写 stdout（对齐 `RuntimeTool` `onApply` 仅 code）。

## Fixture

- `docs/fixtures/electron-next-hostUaReformatRuntime-history-vitest.md`

## 验证

- `HostHistoryRestoreTest` / `UaHistoryRestoreTest` / `ReformatHistoryMetadataTest` / `ReformatHistoryRestoreTest` / `CodeRunHistoryMetadataTest` / `CodeRunHistoryRestoreTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**865/865** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 867 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、JSON/随手记/QR 历史 metadata 提取（见 [DIFF-538](538-json-quicknote-qr-history-git-slice.md)）、Vault Git UI 大改、Compose 与 Electron  substantial UI/引擎差、目标未达成。
