# DIFF-299：JSON 工具栏「清空」对齐 Electron

## 背景

Electron `JsonToolbar` `onClear` 仅 `update({ content: '' })`，不改 `notice`、不 toast。Compose 此前额外写入 `json.notice.cleared` 到状态栏/检查器结果链。

`EditorBuffer.setText` 不触发 `onUserDocumentChange`，故清空后 `notice` 自然保留（与 Electron 一致）。

## 行为

- **F04**：工具栏清空：编辑器置空、`pathResult` 仍经 `clearJsonPathQueryResult()` 丢弃（Compose 检查器状态）；**不**设置 `session.notice`。
- `jsonPath`、查找条、其它会话字段不变。

## 验证

- 对照 `next/src/features/json/JsonTool.tsx` `onClear={() => update({ content: '' })}`
- `./gradlew :composeApp:desktopTest --offline`
