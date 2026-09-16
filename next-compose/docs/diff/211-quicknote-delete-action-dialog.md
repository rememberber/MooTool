# DIFF-211：随手记删除走统一 Action 对话框

## 背景

Electron 随手记树删除经 `openTreeAction(..., 'delete')`，在 `ActionDialog` 中展示 `quickNote.confirmDelete` 与危险「删除」按钮。

DIFF-209 为 JSON/随手记共用 `VaultDeleteConfirmOverlay`；随手记与 Electron 其它新建/重命名/移动对话框形态不一致。

## 行为

- F01：随手记 Vault 删除（右键与当前文件工具栏）改为 `dialogMode = delete` + 现有 `MooOverlay` 对话框。
- JSON Vault 仍使用 `VaultDeleteConfirmOverlay`（`json.vault.confirmDelete`）。
- i18n 增加 `quickNote.delete`（对话框标题与确认钮）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
