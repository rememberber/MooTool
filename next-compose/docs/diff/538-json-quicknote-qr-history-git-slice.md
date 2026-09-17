# DIFF-538：JSON/随手记/QR 历史 metadata + Vault Git 面板 helper + QR 恢复 PNG

## 背景

DIFF-537「未做」仍列 JSON/随手记/QR 历史 options 与 restore 散落在 Screen；Vault Git init/discard/abort 可见性未集中到 `GitOperationPresentation`；F19 历史恢复生成 Tab 未恢复 PNG（Electron 写 data URL 到 `outputText`）。

## 行为

### F04 JSON

- `JsonHistoryMetadata` / `JsonHistoryRestore`：`editor` vs `pathQuery` marker；JSONPath 历史只恢复 `pathResult`/对话框，不改编辑器；其余写回 `output`（对齐 Electron `onApplyHistory`）。

### F01 随手记

- `QuickNoteHistoryMetadata` / `QuickNoteHistoryRestore`：保存时 `options`=相对路径；恢复仅正文，不写 restored notice。

### F19 QR

- `QrHistoryMetadata` / `QrHistoryRestore`：对齐 Electron `extraData` JSON；生成历史 `output` 存 PNG data URL；恢复识别/生成 Tab 与 PNG（旧 `${size}x${size} PNG` 摘要可重算）。

### Vault Git UI

- `GitOperationPresentation`：`initEnabled`、`discardEnabled`、`showAbortAction`；`VaultGitDialog` init/discard/abort 按钮改用上述 helper。

## Fixture

- `docs/fixtures/electron-next-jsonQuickNoteQr-history-vitest.md`

## 验证

- `JsonHistoryMetadataTest` / `JsonHistoryRestoreTest` / `QuickNoteHistoryRestoreTest` / `QrHistoryRestoreTest` / `GitOperationPresentationTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**873/873** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 875 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、Compose 与 Electron substantial UI/引擎差（非历史模块范围）、Vault Git UI 大改、目标未达成。
