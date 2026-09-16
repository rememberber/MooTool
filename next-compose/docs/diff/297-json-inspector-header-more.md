# DIFF-297：JSON 检查器侧栏标题对齐 Electron

## 问题

Electron `JsonInspector` 顶栏标题为 `json.action.more`（与工具栏「更多/检查器」入口同一文案）。Compose `InspectorPane` 使用 `json.panel.inspector`。

## 行为

- 检查器侧栏顶行标题改为 `json.action.more`；溢出菜单项仍用 `json.panel.inspector` 描述切换检查器。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
