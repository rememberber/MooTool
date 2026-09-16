# DIFF-303：通用历史恢复不写 `json.notice.restored`

## 背景

Electron `HistoryDialog` / 各工具 `onApply` / `onApplyRecord` 仅恢复字段并关闭对话框，无「已从历史恢复」类状态栏文案（见 `next/src/features/history/HistoryDialog.tsx` 与各 `*Tool.tsx`）。Compose 在 JSON/随手记（[DIFF-300](300-json-history-replace-copy-state.md)、[DIFF-302](302-quicknote-history-restore-notice.md)）与 Host 已对齐后，其余声明通用历史的工具仍统一写入 `json.notice.restored`。

## 行为

- **A02 / F02–F25（含通用历史）**：`HistoryBrowser.onRestore` 及二维码历史列表点击恢复：只执行各工具既有 `applyHistory` / 字段回填；**不再**设置 `json.notice.restored`。
- 图片/PDF 历史恢复：不写状态栏 `notice`（见 [DIFF-305](305-quicknote-find-notice-image-pdf-history.md)）。
- `json.notice.restored` 键保留供其它场景或 i18n，当前无恢复路径写入。

## 验证

- 对照 `next/src/features/history/HistoryDialog.tsx` 与 `RuntimeTool` / `RegexTool` / `ReformatTool` 等 `onApply`
- `./gradlew :composeApp:desktopTest --offline`
