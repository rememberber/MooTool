# DIFF-483：JSON 检查器缩进选项规范化（对齐 Electron `JsonInspector`）

## 背景

Electron 检查器缩进仅为 `<select>` 的 2 或 4（`JsonInspector.tsx`）。Compose 侧栏为 `MooSegmented` 同样只提供 2/4，但会话快照/迁入若带上 `spaces: 0`（例如误用 MCP 紧凑语义）或其它非法值，分段控件与 `formatAdvanced` 行为会与 Electron 不一致。

## 行为

- `JsonFormatOptions.normalizeInspectorIndent()`：仅保留 `4`，其余一律 `2`。
- `JsonSession.restore` 写回 `formatOptions` 前规范化缩进。

## 验证

- `JsonFormatOptionsTest`
- `./gradlew :composeApp:desktopTest --offline`
