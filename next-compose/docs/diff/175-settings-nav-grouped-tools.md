# DIFF-175：设置导航工具按分组展示

## 背景

Electron `LayoutSettings` 在「导航中的工具」下按 `toolGroups` 分段列出工具，每行含图标与勾选框。compose 此前为扁平 Toggle 列表。

## 行为

- **A01**：`NavigationToolVisibilityList` 按 `ToolRegistry.groups` 渲染分组标题 + 图标/名称/开关，与侧栏分组一致。

## 验证

- 对照 `next/src/features/settings/SettingsWindow.tsx` `navigation-tool-visibility__groups`
- `NavigationToolVisibilityTest`（25 工具、全部显示/隐藏列表）
- `./gradlew :composeApp:desktopTest --offline`
