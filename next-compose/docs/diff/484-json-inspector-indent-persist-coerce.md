# DIFF-484：JSON 检查器缩进持久化与内存校正（对齐 Electron `JsonInspector`）

## 背景

[DIFF-483](483-json-inspector-format-indent-normalize.md) 已在 `JsonSession.restore` 将会话快照中的非法 `spaces` 规范为 2 或 4。若内存中 `formatOptions` 被写成 MCP 语义 `0` 或其它值，仍可能写入磁盘、或导致 `MooSegmented` 与「应用格式」行为不一致。

## 行为

- `JsonSession.snapshot()` / `persistJson()` 写盘前经 `normalizeInspectorIndent()`，仅持久化 2 或 4。
- `coerceInspectorFormatOptions()` 校正内存中的非法缩进；加载旧会话若磁盘 `spaces` 非法，恢复后立即回写修复。
- 检查器分段控件绑定规范化后的值，变更时同样规范化。

## 验证

- `JsonFormatOptionsTest`
- `./gradlew :composeApp:desktopTest --offline`
