# DIFF-368：JSON Vault flush 失败 `notice` 统一辅助函数

## 背景

历史恢复、工具栏导入、Vault 树打开/拖移/多文件拖放等路径重复 `flushJsonVaultEditorIfDirty` + `session.notice = ...` 模式，易漏改。

## 变更

- 新增 `jsonVaultFlushDirtyOrNotice`（`JsonVaultSession.kt`）：失败写 `json.notice.failed` 或异常文案并返回 `false`。
- 新增 `jsonVaultRequireFlushDirty`：供 `runCatching` 块内 flush，失败抛异常且 `notice` 已写入。
- `JsonScreen` 全部 flush 路径（历史、导入、Vault 树、对话框新建/移动/重命名、结果/转换弹层、编辑器拖入）改用上述辅助函数，不再直接调用 `flushJsonVaultEditorIfDirty`。
- 新增 `JsonVaultFlushNoticeTest`（干净跳过 / 冲突写 notice）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
