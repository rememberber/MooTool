# DIFF-305：随手记替换 notice + 图片/PDF 历史恢复

## 背景

- 随手记「全部替换」走 `setText`，不触发 `onUserDocumentChange`；[DIFF-301](301-host-quicknote-replace-all-notice.md) 写 `notice = ""` 会在列编辑闩锁时误清 `quickNote.columnEdit.hint`（JSON 无闩锁故写空串合理）。
- [DIFF-303](303-history-restore-no-restored-notice.md) 后图片/PDF 仍把 `summary`/`output` 写入状态栏；Electron 无对应 restored 文案，与其它工具历史恢复一致应保留既有 `notice`。

## 行为

- **F01**：查找「替换」「全部替换」成功：`notice = quickNoteNoticeOnUserDocumentChange(columnLatch, hint)`（闩锁时保留列编辑提示）。
- **F22/F23**：`HistoryBrowser` 恢复仅回填 `lastOutputs`/选中资源，不写 `session.notice`。

## 验证

- `QuickNoteNoticeTest`
- `./gradlew :composeApp:desktopTest --offline`
