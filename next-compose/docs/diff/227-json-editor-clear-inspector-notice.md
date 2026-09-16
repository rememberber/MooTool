# DIFF-227：JSON 编辑时清除检查器 notice/pathResult

## 背景

Electron `JsonTool` 在编辑器 `onChange` 时清空 `notice`（见 `next/src/features/json/JsonTool.tsx`）。Compose 在 DIFF-194/225/226 引入 `pathResult` 后，用户改文档仍可能看到过期 JSONPath 结果或旧提示。

## 行为

- `EditorBuffer.onUserDocumentChange`：文档监听触发，`setText` 批量加载不触发。
- `JsonScreen` 绑定：清空 `pathResult`；非列编辑闩锁时清空 `notice`；重置复制反馈。
- `loadJsonVaultSnippet` 打开 Vault 文件时清空 `pathResult`/`notice`（对齐 `openVaultContent`）。
- `transform` / 非 JSONPath 的 `showResult` 成功时清空 `pathResult`。
- `clearJsonPathQueryResult()`：导入、历史恢复、查找替换、冲突重载、拖放等经 `setText` 替换正文的路径显式调用。

## 验证

- `EditorBufferUserDocumentChangeTest`
- `./gradlew :composeApp:desktopTest --offline`
